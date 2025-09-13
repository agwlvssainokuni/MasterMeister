/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import axios from 'axios'
import {API_BASE_URL, API_ENDPOINTS} from '../config/config'
import type {ApiResponse, LoginResponse, RefreshTokenRequest} from "../types/api"
import {isTokenExpiringSoon} from '../utils/jwt'

class TokenManager {
  private accessToken: string | null = null

  setTokens(accessToken: string, refreshToken: string): void {
    // アクセストークンはメモリのみに保存（XSS攻撃から保護）
    this.accessToken = accessToken

    // リフレッシュトークンのみ永続化
    localStorage.setItem('refreshToken', refreshToken)
  }

  getAccessToken(): string | null {
    return this.accessToken
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken')
  }

  clearTokens(): void {
    this.accessToken = null
    localStorage.removeItem('refreshToken')
  }

  hasValidAccessToken(): boolean {
    return this.accessToken !== null && !isTokenExpiringSoon(this.accessToken)
  }

  hasRefreshToken(): boolean {
    return this.getRefreshToken() !== null
  }
}

// シングルトンインスタンス
const tokenManager = new TokenManager()

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30秒タイムアウト
})

// Callbacks for handling auth events
let onAuthFailure: (() => void) | null = null
let onTokenRefresh: ((accessToken: string, refreshToken: string) => void) | null = null

export const setAuthFailureHandler = (handler: () => void) => {
  onAuthFailure = handler
}

export const setTokenRefreshHandler = (handler: (accessToken: string, refreshToken: string) => void) => {
  onTokenRefresh = handler
}

// Global flag to prevent concurrent refresh attempts
let isRefreshing = false
// Promise to handle concurrent refresh requests
let refreshPromise: Promise<LoginResponse | undefined> | null = null

// Background refresh function
const refreshTokenInBackground = async (): Promise<LoginResponse | undefined> => {
  if (isRefreshing && refreshPromise) {
    return refreshPromise
  }

  const refreshToken = tokenManager.getRefreshToken()
  if (!refreshToken) {
    return undefined
  }

  isRefreshing = true
  refreshPromise = (async () => {
    try {
      const response = await axios.post<ApiResponse<LoginResponse>>(
        `${API_BASE_URL}${API_ENDPOINTS.AUTH.REFRESH}`,
        {refreshToken} as RefreshTokenRequest,
        {timeout: 10000} // リフレッシュは10秒タイムアウト
      )

      const {data} = response.data as ApiResponse<LoginResponse>
      if (!data) {
        return undefined
      }

      // TokenManagerを使用してセキュアに保存
      tokenManager.setTokens(data.accessToken, data.refreshToken)

      // Notify AuthContext of successful refresh
      onTokenRefresh?.(data.accessToken, data.refreshToken)

      return data
    } catch (error) {
      console.warn('Background token refresh failed:', error)
      // リフレッシュ失敗時はトークンをクリア
      tokenManager.clearTokens()
      return undefined
    } finally {
      isRefreshing = false
      refreshPromise = null
    }
  })()

  return refreshPromise
}

apiClient.interceptors.request.use(
  async (config) => {
    // 有効なアクセストークンがある場合はそのまま使用
    if (tokenManager.hasValidAccessToken()) {
      const token = tokenManager.getAccessToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    }

    // アクセストークンが期限切れまたは存在しない場合、リフレッシュトークンで取得を試行
    if (tokenManager.hasRefreshToken()) {
      try {
        const loginResult = await refreshTokenInBackground()
        if (loginResult?.accessToken) {
          config.headers.Authorization = `Bearer ${loginResult.accessToken}`
          return config
        }
      } catch (error) {
        console.warn('Proactive token refresh failed in request interceptor:', error)
        // 認証失敗を通知してログイン画面へ
        onAuthFailure?.()
        return Promise.reject(new Error('Authentication required'))
      }
    }

    // トークンが存在しない場合はそのまま送信（認証不要なエンドポイント用）
    return config
  },
  (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle authentication failures - clean up and redirect to login
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.warn('Authentication failed:', error.response?.status, error.response?.statusText)

      // TokenManagerでトークンをクリア
      tokenManager.clearTokens()

      // Notify auth failure for redirect to login
      onAuthFailure?.()
    }

    return Promise.reject(error)
  }
)

// TokenManagerインスタンスをエクスポート（外部からアクセス可能にする）
export {tokenManager}

export default apiClient

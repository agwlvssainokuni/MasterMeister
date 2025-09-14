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
import {API_BASE_URL, API_CONFIG, API_ENDPOINTS} from '../config/config'
import type {ApiResponse, LoginResponse, RefreshTokenRequest} from "../types/api"
import {tokenManager} from './tokenManager'

// === API Client Configuration ===
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: API_CONFIG.DEFAULT_TIMEOUT_MS,
})

// === Auth Event Handlers ===
let onAuthFailure: (() => void) | null = null

export const setAuthFailureHandler = (handler: () => void) => {
  onAuthFailure = handler
}

// === Token Refresh Management ===
let isRefreshing = false
let refreshPromise: Promise<LoginResponse | undefined> | null = null

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
        {timeout: API_CONFIG.REFRESH_TIMEOUT_MS}
      )

      const {data} = response.data as ApiResponse<LoginResponse>
      if (!data) {
        return undefined
      }

      // TokenManagerを使用してセキュアに保存（自動的にリスナーに通知される）
      tokenManager.setTokens(data.accessToken, data.refreshToken)

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

// === Request Interceptor ===
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

// === Response Interceptor ===
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

// === Exports ===
export default apiClient

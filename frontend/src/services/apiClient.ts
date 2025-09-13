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
const refreshTokenInBackground = async (refreshToken: string): Promise<LoginResponse | undefined> => {
  if (isRefreshing && refreshPromise) {
    return refreshPromise
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
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)

      // Notify AuthContext of successful refresh
      onTokenRefresh?.(data.accessToken, data.refreshToken)

      return data
    } catch (error) {
      console.warn('Background token refresh failed:', error)
      // リフレッシュ失敗時はトークンをクリア
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
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
    const token = localStorage.getItem('accessToken')
    const refreshToken = localStorage.getItem('refreshToken')

    // Proactively refresh if token is expiring soon
    if (token && refreshToken && isTokenExpiringSoon(token)) {
      try {
        const loginResult = await refreshTokenInBackground(refreshToken)
        if (loginResult?.accessToken) {
          config.headers.Authorization = `Bearer ${loginResult.accessToken}`
          return config
        }
      } catch (error) {
        console.warn('Proactive token refresh failed in request interceptor:', error)
        // Fall through to use existing token or no token
      }
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

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

      // Clean up tokens
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')

      // Notify auth failure for redirect to login
      onAuthFailure?.()
    }

    return Promise.reject(error)
  }
)

export default apiClient

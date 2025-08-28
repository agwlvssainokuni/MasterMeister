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
import type {ApiResponse, LoginResult, RefreshTokenRequest} from "../types/api"
import {isTokenExpiringSoon} from '../utils/jwt'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Global flag to prevent concurrent refresh attempts
let isRefreshing = false

// Background refresh function
const refreshTokenInBackground = async (refreshToken: string) => {
  if (isRefreshing) return

  isRefreshing = true
  try {
    const response = await axios.post<ApiResponse<LoginResult>>(
      `${API_BASE_URL}${API_ENDPOINTS.AUTH.REFRESH}`, {
        refreshToken
      } as RefreshTokenRequest
    )

    const {data} = response.data
    if (data) {
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)

      // Notify AuthContext of successful refresh
      onTokenRefresh?.(data.accessToken, data.refreshToken)
    }
  } catch (error) {
    console.warn('Background token refresh failed:', error)
  } finally {
    isRefreshing = false
  }
}

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    const refreshToken = localStorage.getItem('refreshToken')

    // Trigger background refresh if token is expiring soon
    if (token && refreshToken && !isRefreshing && isTokenExpiringSoon(token)) {
      refreshTokenInBackground(refreshToken).catch(() => {
        // Ignore errors - response interceptor will handle them
      })
    }

    if (token) {
      config.headers = {
        ...config.headers,
        Authorization: `Bearer ${token}`,
      }
    }

    return config
  },
  (error) => Promise.reject(error)
)

// Callbacks for handling auth events
let onAuthFailure: (() => void) | null = null
let onTokenRefresh: ((accessToken: string, refreshToken: string) => void) | null = null

export const setAuthFailureHandler = (handler: () => void) => {
  onAuthFailure = handler
}

export const setTokenRefreshHandler = (handler: (accessToken: string, refreshToken: string) => void) => {
  onTokenRefresh = handler
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Handle 401 (unauthorized) and 403 (forbidden/expired token) - try token refresh
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      originalRequest._retry = true

      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken && !isRefreshing) {
        isRefreshing = true
        try {
          const response = await axios.post<ApiResponse<LoginResult>>(
            `${API_BASE_URL}${API_ENDPOINTS.AUTH.REFRESH}`, {
              refreshToken
            } as RefreshTokenRequest
          )

          const {data} = response.data
          if (data) {
            localStorage.setItem('accessToken', data.accessToken)
            localStorage.setItem('refreshToken', data.refreshToken)

            // Notify AuthContext of successful token refresh
            onTokenRefresh?.(data.accessToken, data.refreshToken)

            originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
            return apiClient(originalRequest)
          }
        } catch {
          // Token refresh failed - redirect to login
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          onAuthFailure?.()
        } finally {
          isRefreshing = false
        }
      } else {
        // No refresh token or already refreshing - redirect to login
        onAuthFailure?.()
      }
    }

    return Promise.reject(error)
  }
)

export default apiClient

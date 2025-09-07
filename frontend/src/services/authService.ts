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

import apiClient from './apiClient'
import {API_ENDPOINTS} from '../config/config'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  LogoutRequest,
  RefreshTokenRequest,
  RegisterEmailRequest,
  RegisterEmailResponse as ApiRegisterEmailResult,
  RegisterUserRequest,
  RegisterUserResponse as ApiRegisterUserResult
} from '../types/api'
import type {
  AuthState,
  LoginCredentials,
  RegisterEmailCredentials,
  RegisterEmailResult,
  RegisterUserCredentials,
  RegisterUserResult,
  User
} from '../types/frontend'
import {extractUserFromToken, isTokenExpired} from '../utils/jwt'

class AuthService {
  async login(credentials: LoginCredentials): Promise<AuthState> {
    const loginRequest: LoginRequest = {
      email: credentials.email,
      password: credentials.password
    }

    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      API_ENDPOINTS.AUTH.LOGIN,
      loginRequest
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Login failed')
    }

    const loginResult = response.data.data

    // Store tokens
    localStorage.setItem('accessToken', loginResult.accessToken)
    localStorage.setItem('refreshToken', loginResult.refreshToken)

    // Convert to frontend types
    const user: User = {
      email: loginResult.email,
      role: loginResult.role as 'USER' | 'ADMIN'
    }

    return {
      isAuthenticated: true,
      user,
      accessToken: loginResult.accessToken,
      refreshToken: loginResult.refreshToken
    }
  }

  async logout(): Promise<void> {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      const logoutRequest: LogoutRequest = {refreshToken}

      try {
        await apiClient.post<ApiResponse<string>>(
          API_ENDPOINTS.AUTH.LOGOUT,
          logoutRequest
        )
      } catch (error) {
        // Continue with logout even if API call fails
        console.warn('Logout API call failed:', error)
      }
    }

    // Clear local storage
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async refreshAccessToken(): Promise<AuthState> {
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      throw new Error('No refresh token available')
    }

    const refreshRequest: RefreshTokenRequest = {
      refreshToken
    }

    try {
      const response = await apiClient.post<ApiResponse<LoginResponse>>(
        API_ENDPOINTS.AUTH.REFRESH,
        refreshRequest
      )

      if (!response.data.ok || !response.data.data) {
        throw new Error(response.data.error?.[0] || 'Token refresh failed')
      }

      const loginResult = response.data.data

      // Update stored tokens
      localStorage.setItem('accessToken', loginResult.accessToken)
      localStorage.setItem('refreshToken', loginResult.refreshToken)

      // Convert to frontend types
      const user: User = {
        email: loginResult.email,
        role: loginResult.role as 'USER' | 'ADMIN'
      }

      return {
        isAuthenticated: true,
        user,
        accessToken: loginResult.accessToken,
        refreshToken: loginResult.refreshToken
      }
    } catch (error) {
      // Clear invalid tokens
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      throw error
    }
  }

  async registerEmail(credentials: RegisterEmailCredentials): Promise<RegisterEmailResult> {
    const request: RegisterEmailRequest = {
      email: credentials.email,
      language: credentials.language || 'ja',
    }

    const response = await apiClient.post<ApiResponse<ApiRegisterEmailResult>>(
      API_ENDPOINTS.USERS.REGISTER_EMAIL,
      request
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Email registration failed')
    }

    return response.data.data
  }

  async registerUser(credentials: RegisterUserCredentials): Promise<RegisterUserResult> {
    const request: RegisterUserRequest = {
      token: credentials.token,
      email: credentials.email,
      password: credentials.password,
      language: credentials.language || 'ja',
    }

    const response = await apiClient.post<ApiResponse<ApiRegisterUserResult>>(
      API_ENDPOINTS.USERS.REGISTER,
      request
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'User registration failed')
    }

    const apiResult = response.data.data
    return {
      userId: apiResult.userId,
      email: apiResult.email
    }
  }

  getCurrentAuthState(): AuthState {
    const accessToken = localStorage.getItem('accessToken')
    const refreshToken = localStorage.getItem('refreshToken')

    if (!accessToken || !refreshToken) {
      return {
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null
      }
    }

    // Check if token is expired
    if (isTokenExpired(accessToken)) {
      return {
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null
      }
    }

    // Extract user info from JWT
    const userInfo = extractUserFromToken(accessToken)
    const user: User | null = userInfo ? {
      email: userInfo.email,
      role: userInfo.role
    } : null

    return {
      isAuthenticated: !!userInfo,
      user,
      accessToken,
      refreshToken
    }
  }
}

export const authService = new AuthService()

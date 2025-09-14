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
import {tokenManager} from './tokenManager'
import {API_ENDPOINTS} from '../config/config'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  LogoutRequest,
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
  RegisterUserResult
} from '../types/frontend'

class AuthService {
  // === Core Authentication Methods ===
  async login(credentials: LoginCredentials): Promise<AuthState> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      API_ENDPOINTS.AUTH.LOGIN,
      {
        email: credentials.email,
        password: credentials.password
      } as LoginRequest
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Login failed')
    }

    const loginResult = response.data.data

    // Store tokens using TokenManager
    tokenManager.setTokens(loginResult.accessToken, loginResult.refreshToken)

    return tokenManager.createAuthStateFromTokens(
      loginResult.accessToken,
      loginResult.refreshToken
    )
  }

  async logout(): Promise<void> {
    const refreshToken = tokenManager.getRefreshToken()
    if (refreshToken) {
      try {
        await apiClient.post<ApiResponse<string>>(
          API_ENDPOINTS.AUTH.LOGOUT,
          {
            refreshToken
          } as LogoutRequest
        )
      } catch (error) {
        // Continue with logout even if API call fails
        console.warn('Logout API call failed:', error)
      }
    }

    // Clear tokens using TokenManager
    tokenManager.clearTokens()
  }

  // === User Registration Methods ===
  async registerEmail(credentials: RegisterEmailCredentials): Promise<RegisterEmailResult> {
    const response = await apiClient.post<ApiResponse<ApiRegisterEmailResult>>(
      API_ENDPOINTS.USERS.REGISTER_EMAIL,
      {
        email: credentials.email,
        language: credentials.language || 'ja',
      } as RegisterEmailRequest
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Email registration failed')
    }

    return response.data.data
  }

  async registerUser(credentials: RegisterUserCredentials): Promise<RegisterUserResult> {
    const response = await apiClient.post<ApiResponse<ApiRegisterUserResult>>(
      API_ENDPOINTS.USERS.REGISTER,
      {
        token: credentials.token,
        email: credentials.email,
        password: credentials.password,
        language: credentials.language || 'ja',
      } as RegisterUserRequest
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
}

// === Export ===
export const authService = new AuthService()

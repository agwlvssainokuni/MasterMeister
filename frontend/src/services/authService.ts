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
  EmailConfirmationRequest,
  EmailConfirmationResult,
  LoginRequest,
  LoginResult,
  LogoutRequest,
  UserRegistrationRequest,
  UserRegistrationResult
} from '../types/api'
import type {AuthState, LoginCredentials, RegistrationCredentials, User} from '../types/frontend'
import {extractUserFromToken, isTokenExpired} from '../utils/jwt'

class AuthService {
  async login(credentials: LoginCredentials): Promise<AuthState> {
    const loginRequest: LoginRequest = {
      username: credentials.username,
      password: credentials.password
    }

    const response = await apiClient.post<ApiResponse<LoginResult>>(
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
      username: loginResult.username,
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

  async register(credentials: RegistrationCredentials): Promise<UserRegistrationResult> {
    const registerRequest: UserRegistrationRequest = {
      username: credentials.username,
      email: credentials.email,
      password: credentials.password,
      fullName: credentials.fullName,
      language: navigator.language.startsWith('ja') ? 'ja' : 'en'
    }

    const response = await apiClient.post<ApiResponse<UserRegistrationResult>>(
      API_ENDPOINTS.USERS.REGISTER,
      registerRequest
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Registration failed')
    }

    return response.data.data
  }

  async confirmEmail(token: string): Promise<EmailConfirmationResult> {
    const request: EmailConfirmationRequest = {token}

    const response = await apiClient.post<ApiResponse<EmailConfirmationResult>>(
      API_ENDPOINTS.USERS.CONFIRM_EMAIL,
      request
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Email confirmation failed')
    }

    return response.data.data
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
      username: userInfo.username,
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

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

// API DTOs - Backend 1:1 correspondence

// Common Response
export interface ApiResponse<T> {
  ok: boolean
  data?: T
  error?: string[]
}

// AuthController
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  email: string
  role: string
  expiresIn: number
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

// AdminController
export interface UserSummaryResult {
  id: number
  email: string
  status: string
  emailConfirmed: boolean
  createdAt: string
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface ApproveUserRequest {
  // No body required
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface RejectUserRequest {
  // No body required
}

// User Registration - Email First Flow
export interface RegisterEmailRequest {
  email: string
  language: string
}

export interface RegisterEmailResult {
  email: string
}

export interface RegisterUserRequest {
  token: string
  email: string
  password: string
  language: string
}

export interface RegisterUserResult {
  userId: number
  email: string
}

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

// Frontend types - optimized for UI state management

export interface User {
  username: string
  role: 'USER' | 'ADMIN'
}

export interface AuthState {
  isAuthenticated: boolean
  user: User | null
  accessToken: string | null
  refreshToken: string | null
}

export interface LoginCredentials {
  username: string
  password: string
}

export interface PendingUser {
  id: number
  username: string
  email: string
  registeredAt: Date
  isPending?: boolean
}

export interface AdminAction {
  type: 'approve' | 'reject'
  userId: number
  username: string
}

export interface RegistrationCredentials {
  username: string
  email: string
  password: string
  confirmPassword: string
  fullName: string
}

export interface RegistrationResult {
  userId: number
  username: string
  email: string
  message: string
}

export interface EmailConfirmationResult {
  status: string
  message: string
}

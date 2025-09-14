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

import {API_CONFIG} from '../config/config'
import type {AuthState, User} from '../types/frontend'
import {extractUserFromToken, isTokenExpired, isTokenExpiringSoon} from '../utils/jwt'

class TokenManager {
  private accessToken: string | null = null
  private tokenValidUntil: number = 0 // トークン検証キャッシュ
  private refreshListeners: Array<(authState: AuthState) => void> = []

  // === Event Management ===
  addRefreshListener(listener: (authState: AuthState) => void): void {
    this.refreshListeners.push(listener)
  }

  private notifyRefreshListeners(accessToken: string, refreshToken: string): void {
    const newAuthState = this.createAuthStateFromTokens(accessToken, refreshToken)
    this.refreshListeners.forEach(listener => listener(newAuthState))
  }

  // === Core Token Operations ===
  setTokens(accessToken: string, refreshToken: string): void {
    // アクセストークンはメモリのみに保存（XSS攻撃から保護）
    this.accessToken = accessToken

    // トークン更新時はキャッシュをクリア
    this.tokenValidUntil = 0

    // リフレッシュトークンのみ永続化
    localStorage.setItem('refreshToken', refreshToken)

    // トークン更新をリスナーに通知
    this.notifyRefreshListeners(accessToken, refreshToken)
  }

  clearTokens(): void {
    this.accessToken = null
    this.tokenValidUntil = 0 // キャッシュもクリア
    localStorage.removeItem('refreshToken')
  }

  // === Token Getters ===
  getAccessToken(): string | null {
    return this.accessToken
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken')
  }

  // === Token Validation ===
  hasValidAccessToken(): boolean {
    if (!this.accessToken) {
      return false
    }

    const now = Date.now()

    // キャッシュが有効な場合はキャッシュ結果を使用
    if (now < this.tokenValidUntil) {
      return true
    }

    // トークン検証を実行
    const isValid = !isTokenExpiringSoon(this.accessToken)

    // 検証結果が有効な場合はキャッシュに保存
    if (isValid) {
      this.tokenValidUntil = now + API_CONFIG.VALIDATION_CACHE_DURATION_MS
    }

    return isValid
  }

  hasRefreshToken(): boolean {
    return this.getRefreshToken() !== null
  }

  // === State Management ===
  getCurrentAuthState(): AuthState {
    const accessToken = this.getAccessToken()
    const refreshToken = this.getRefreshToken()

    if (!accessToken || !refreshToken) {
      return {
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null
      }
    }

    // 実際の状態構築は共通メソッドに委譲
    return this.createAuthStateFromTokens(accessToken, refreshToken)
  }

  createAuthStateFromTokens(accessToken: string, refreshToken: string): AuthState {
    if (isTokenExpired(accessToken)) {
      return {
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null
      }
    }

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

// === Export ===
export const tokenManager = new TokenManager()

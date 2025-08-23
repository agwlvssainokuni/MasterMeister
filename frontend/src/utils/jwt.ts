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

interface JWTPayload {
  sub: string // userUuid
  email: string // email address
  role: string[] // roles
  exp: number // expiration timestamp
  iat: number // issued at timestamp
  jti?: string // JWT ID
}

export const parseJWT = (token: string): JWTPayload | null => {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }

    const payload = parts[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded) as JWTPayload
  } catch (error) {
    console.warn('Failed to parse JWT:', error)
    return null
  }
}

export const isTokenExpired = (token: string): boolean => {
  const payload = parseJWT(token)
  if (!payload) {
    return true
  }

  const now = Math.floor(Date.now() / 1000)
  return payload.exp < now
}

export const extractUserFromToken = (token: string): { email: string; role: 'USER' | 'ADMIN' } | null => {
  const payload = parseJWT(token)
  if (!payload) {
    return null
  }

  // Extract first role from role array (already cleaned of ROLE_ prefix by backend)
  const role = payload.role?.[0] as 'USER' | 'ADMIN'

  if (!role || !payload.email) {
    return null
  }

  return {
    email: payload.email,
    role
  }
}
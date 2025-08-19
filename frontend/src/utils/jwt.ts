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
  sub: string // username
  authorities: string[] // roles
  exp: number // expiration timestamp
  iat: number // issued at timestamp
  jti?: string // JWT ID
}

export function parseJWT(token: string): JWTPayload | null {
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

export function isTokenExpired(token: string): boolean {
  const payload = parseJWT(token)
  if (!payload) {
    return true
  }

  const now = Math.floor(Date.now() / 1000)
  return payload.exp < now
}

export function extractUserFromToken(token: string): { username: string; role: 'USER' | 'ADMIN' } | null {
  const payload = parseJWT(token)
  if (!payload) {
    return null
  }

  // Extract role from authorities array (format: "ROLE_USER" or "ROLE_ADMIN")
  const role = payload.authorities
    ?.find(auth => auth.startsWith('ROLE_'))
    ?.replace('ROLE_', '') as 'USER' | 'ADMIN'

  if (!role) {
    return null
  }

  return {
    username: payload.sub,
    role
  }
}
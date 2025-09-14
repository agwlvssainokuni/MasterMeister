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

import type {ReactNode} from 'react'
import {createContext, useCallback, useContext, useEffect, useState} from 'react'
import {useNavigate} from 'react-router-dom'
import type {AuthState, LoginCredentials} from '../types/frontend'
import {authService} from '../services/authService'
import {setAuthFailureHandler} from '../services/apiClient'
import {tokenManager} from '../services/tokenManager'
import {useNotification} from './NotificationContext'

interface AuthContextValue extends AuthState {
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  isLoading: boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

export const AuthProvider = ({children}: AuthProviderProps) => {
  // === State Management ===
  const [authState, setAuthState] = useState<AuthState>(() =>
    tokenManager.getCurrentAuthState()
  )
  const [isLoading, setIsLoading] = useState(false)
  const navigate = useNavigate()
  const {showError} = useNotification()

  // === Auth Failure Handler ===
  const handleAuthFailure = useCallback(() => {
    setAuthState({
      isAuthenticated: false,
      user: null,
      accessToken: null,
      refreshToken: null
    })
    showError('Session expired. Please log in again.')
    navigate('/login', {replace: true})
  }, [navigate, showError])

  // === Initialization ===
  useEffect(() => {
    // Initialize auth state on mount
    const currentState = tokenManager.getCurrentAuthState()
    setAuthState(currentState)

    // Setup auth failure handler for API client
    setAuthFailureHandler(handleAuthFailure)

    // Setup token refresh listener with TokenManager
    const refreshListener = (newAuthState: AuthState) => {
      if (newAuthState.isAuthenticated) {
        setAuthState(newAuthState)
        // Optionally show success message (commented out to avoid noise)
        // showInfo('Session refreshed successfully.')
      }
    }

    tokenManager.addRefreshListener(refreshListener)

    // Cleanup function to prevent memory leaks
    return () => {
      tokenManager.removeRefreshListener(refreshListener)
    }
  }, [handleAuthFailure])

  // === Authentication Actions ===
  const login = async (credentials: LoginCredentials) => {
    setIsLoading(true)
    try {
      const newAuthState = await authService.login(credentials)
      setAuthState(newAuthState)
    } catch (error) {
      setAuthState({
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null,
      })
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const logout = async () => {
    setIsLoading(true)
    try {
      await authService.logout()
      setAuthState({
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null,
      })
    } catch (error) {
      console.warn('Logout failed:', error)
      // Clear state anyway
      setAuthState({
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null,
      })
    } finally {
      setIsLoading(false)
    }
  }

  // === Context Value ===
  const contextValue: AuthContextValue = {
    ...authState,
    login,
    logout,
    isLoading
  }

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

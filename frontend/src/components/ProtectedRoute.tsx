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
import {Navigate, useLocation} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'

interface ProtectedRouteProps {
  children: ReactNode
  requiredRole?: 'USER' | 'ADMIN'
  fallback?: ReactNode
}

export const ProtectedRoute = (
  {children, requiredRole, fallback}: ProtectedRouteProps,
) => {
  const {isAuthenticated, user, isLoading} = useAuth()
  const location = useLocation()

  if (isLoading) {
    return fallback || <div>Loading...</div>
  }

  if (!isAuthenticated || !user) {
    // Redirect to login with return path
    return <Navigate to="/login" state={{from: location}} replace/>
  }

  if (requiredRole && user.role !== requiredRole) {
    // User doesn't have required role
    return <Navigate to="/unauthorized" replace/>
  }

  return <>{children}</>
}

interface AdminRouteProps {
  children: ReactNode
  fallback?: ReactNode
}

export const AdminRoute = (
  {children, fallback}: AdminRouteProps,
) => {
  return (
    <ProtectedRoute requiredRole="ADMIN" fallback={fallback}>
      {children}
    </ProtectedRoute>
  )
}

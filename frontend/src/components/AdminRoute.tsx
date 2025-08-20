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
import {Navigate} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'

interface AdminRouteProps {
  children: ReactNode
}

export const AdminRoute = ({children}: AdminRouteProps) => {
  const {isAuthenticated, user} = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace/>
  }

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/dashboard" replace/>
  }

  return <>{children}</>
}

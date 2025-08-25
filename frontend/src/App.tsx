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

import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom'
import {AuthProvider} from './contexts/AuthContext'
import {NotificationProvider} from './contexts/NotificationContext'
import {AdminRoute, ProtectedRoute} from './components/ProtectedRoute'
import {LoginPage} from './pages/LoginPage'
import {RegisterEmailPage} from './pages/RegisterEmailPage.tsx'
import {RegisterUserPage} from './pages/RegisterUserPage.tsx'
import {AdminDashboardPage} from './pages/AdminDashboardPage'
import {UserManagementPage} from './pages/UserManagementPage'
import {DashboardPage} from './pages/DashboardPage'
import {DataAccessPage} from './pages/DataAccessPage'
import {DatabaseConnectionsPage} from './pages/DatabaseConnectionsPage'
import {SchemaManagementPage} from './pages/SchemaManagementPage'
import {PermissionManagementPage} from './pages/PermissionManagementPage'
import './App.css'

const App = () => {
  return (
    <Router>
      <NotificationProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage/>}/>
            <Route path="/register-email" element={<RegisterEmailPage/>}/>
            <Route path="/register" element={<RegisterUserPage/>}/>
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage/>
                </ProtectedRoute>
              }
            />
            <Route
              path="/data"
              element={
                <ProtectedRoute>
                  <DataAccessPage/>
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <AdminRoute>
                  <AdminDashboardPage/>
                </AdminRoute>
              }
            />
            <Route
              path="/admin/users"
              element={
                <AdminRoute>
                  <UserManagementPage/>
                </AdminRoute>
              }
            />
            <Route
              path="/admin/database-connections"
              element={
                <AdminRoute>
                  <DatabaseConnectionsPage/>
                </AdminRoute>
              }
            />
            <Route
              path="/admin/schema"
              element={
                <AdminRoute>
                  <SchemaManagementPage/>
                </AdminRoute>
              }
            />
            <Route
              path="/admin/permissions"
              element={
                <AdminRoute>
                  <PermissionManagementPage/>
                </AdminRoute>
              }
            />
            <Route path="/" element={<Navigate to="/dashboard" replace/>}/>
          </Routes>
        </AuthProvider>
      </NotificationProvider>
    </Router>
  )
}

export default App

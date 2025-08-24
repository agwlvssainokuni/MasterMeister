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

export const API_BASE_URL = '/api'

export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh'
  },
  USERS: {
    REGISTER_EMAIL: '/users/register-email',
    REGISTER: '/users/register'
  },
  ADMIN: {
    PENDING_USERS: '/admin/users/pending',
    APPROVE_USER: (id: number) => `/admin/users/${id}/approve`,
    REJECT_USER: (id: number) => `/admin/users/${id}/reject`
  },
  DATA_ACCESS: {
    ACCESSIBLE_TABLES: (connectionId: number) => `/data/${connectionId}/tables`,
    TABLE_METADATA: (connectionId: number, schemaName: string, tableName: string) => 
      `/data/${connectionId}/tables/${schemaName}/${tableName}/metadata`,
    TABLE_RECORDS: (connectionId: number, schemaName: string, tableName: string) => 
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records`,
    RECORD_CREATE: (connectionId: number, schemaName: string, tableName: string) => 
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records`,
    RECORD_UPDATE: (connectionId: number, schemaName: string, tableName: string) => 
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records`,
    RECORD_DELETE: (connectionId: number, schemaName: string, tableName: string) => 
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records:delete`
  },
  DATABASE_CONNECTIONS: {
    LIST: '/admin/database-connections',
    GET: (id: number) => `/admin/database-connections/${id}`,
    CREATE: '/admin/database-connections',
    UPDATE: (id: number) => `/admin/database-connections/${id}`,
    DELETE: (id: number) => `/admin/database-connections/${id}`,
    TEST: (id: number) => `/admin/database-connections/${id}/test`,
    ACTIVATE: (id: number) => `/admin/database-connections/${id}/activate`,
    DEACTIVATE: (id: number) => `/admin/database-connections/${id}/deactivate`
  },
  SCHEMA: {
    READ: (connectionId: number) => `/admin/schema/${connectionId}`,
    GET_CACHED: (connectionId: number) => `/admin/schema/${connectionId}/cached`,
    REFRESH: (connectionId: number) => `/admin/schema/${connectionId}/refresh`,
    HISTORY: (connectionId: number) => `/admin/schema/${connectionId}/history`,
    FAILURES: (connectionId: number) => `/admin/schema/${connectionId}/failures`
  },
  PERMISSIONS: {
    EXPORT: (connectionId: number) => `/admin/permissions/${connectionId}/export`,
    IMPORT: (connectionId: number) => `/admin/permissions/${connectionId}/import`,
    VALIDATE: (connectionId: number) => `/admin/permissions/${connectionId}/validate`
  }
}

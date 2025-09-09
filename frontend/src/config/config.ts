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

// Authentication configuration
export const AUTH_CONFIG = {
  /**
   * Token refresh buffer time in milliseconds
   *
   * This determines how early the system will proactively refresh tokens
   * before they expire to prevent authentication interruptions.
   *
   * Examples:
   * - 60000 (1 minute): Refresh when 1 minute remaining
   * - 120000 (2 minutes): Refresh when 2 minutes remaining
   * - 300000 (5 minutes): Refresh when 5 minutes remaining
   *
   * Note: Should be less than the token's actual lifetime.
   * Backend tokens typically have 5-minute lifetime.
   */
  TOKEN_REFRESH_BUFFER_MS: 60000, // Default: 1 minute
}

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
    DATABASES: '/data/databases',
    ACCESSIBLE_TABLES: (connectionId: number) => `/data/${connectionId}/tables`,
    TABLE_DETAILS: (connectionId: number, schemaName: string, tableName: string) =>
      `/data/${connectionId}/tables/${schemaName}/${tableName}`,
    TABLE_RECORDS: (connectionId: number, schemaName: string, tableName: string) =>
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records`,
    TABLE_RECORDS_FILTER: (connectionId: number, schemaName: string, tableName: string) =>
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records/filter`,
    RECORD_CREATE: (connectionId: number, schemaName: string, tableName: string) =>
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records`,
    RECORD_UPDATE: (connectionId: number, schemaName: string, tableName: string) =>
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records`,
    RECORD_DELETE: (connectionId: number, schemaName: string, tableName: string) =>
      `/data/${connectionId}/tables/${schemaName}/${tableName}/records:delete`
  },
  DATABASES: {
    LIST: '/admin/databases',
    GET: (id: number) => `/admin/databases/${id}`,
    CREATE: '/admin/databases',
    UPDATE: (id: number) => `/admin/databases/${id}`,
    DELETE: (id: number) => `/admin/databases/${id}`,
    TEST: (id: number) => `/admin/databases/${id}/test`,
    ACTIVATE: (id: number) => `/admin/databases/${id}/activate`,
    DEACTIVATE: (id: number) => `/admin/databases/${id}/deactivate`
  },
  SCHEMA: {
    GET: (connectionId: number) => `/admin/schema/${connectionId}`,
    REFRESH: (connectionId: number) => `/admin/schema/${connectionId}/refresh`,
    HISTORY: (connectionId: number) => `/admin/schema/${connectionId}/history`,
    FAILURES: (connectionId: number) => `/admin/schema/${connectionId}/failures`
  },
  PERMISSIONS: {
    EXPORT: (connectionId: number) => `/admin/permissions/${connectionId}/export`,
    IMPORT: (connectionId: number) => `/admin/permissions/${connectionId}/import`,
    VALIDATE: (connectionId: number) => `/admin/permissions/${connectionId}/validate`,
    BULK_GRANT: (connectionId: number) => `/admin/permissions/${connectionId}/bulk-grant`
  }
}

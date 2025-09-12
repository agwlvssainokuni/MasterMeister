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

// API DTOs - Backend 1:1 correspondence

// Common Response
export interface ApiResponse<T> {
  ok: boolean
  data?: T
  error?: string[]
}

// AuthController
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  email: string
  role: string
  expiresIn: number
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

// AdminController
export interface UserSummaryResponse {
  id: number
  email: string
  status: string
  emailConfirmed: boolean
  createdAt: string
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface ApproveUserRequest {
  // No body required
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface RejectUserRequest {
  // No body required
}

// User Registration - Email First Flow
export interface RegisterEmailRequest {
  email: string
  language: string
}

export interface RegisterEmailResponse {
  email: string
}

export interface RegisterUserRequest {
  token: string
  email: string
  password: string
  language: string
}

export interface RegisterUserResponse {
  userId: number
  email: string
}

// DataAccessController
export interface AccessibleTableResponse {
  connectionId: number
  schemaName: string
  tableName: string
  fullTableName: string
  tableType: string
  comment?: string
  permissions: string[]
  canRead: boolean
  canWrite: boolean
  canDelete: boolean
  canAdmin: boolean
  canModifyData: boolean
  canPerformCrud: boolean
  columns: AccessibleColumnResponse[]
}

export interface AccessibleColumnResponse {
  columnName: string
  dataType: string
  columnSize?: number
  decimalDigits?: number
  nullable: boolean
  defaultValue?: string
  comment?: string
  primaryKey: boolean
  autoIncrement: boolean
  ordinalPosition: number
  permissions: string[]
  canRead: boolean
  canWrite: boolean
  canDelete: boolean
  canAdmin: boolean
}

export interface SchemaMetadataResponse {
  connectionId: number
  databaseName: string
  schemas: string[]
  tables: TableMetadataResponse[]
  lastUpdatedAt: string
}

export interface TableMetadataResponse {
  schema: string
  tableName: string
  tableType: string
  comment?: string
  columns: ColumnMetadataResponse[]
}

export interface ColumnMetadataResponse {
  columnName: string
  dataType: string
  columnSize?: number
  decimalDigits?: number
  nullable: boolean
  defaultValue?: string
  comment?: string
  primaryKey: boolean
  autoIncrement: boolean
  ordinalPosition: number
}

export interface RecordQueryResponse {
  records: Record<string, unknown>[]
  accessibleColumns: AccessibleColumnResponse[]
  totalRecords: number
  currentPage: number
  pageSize: number
  totalPages: number
  hasNextPage: boolean
  hasPreviousPage: boolean
  executionTimeMs: number
  query: string
}

export interface ColumnFilterRequest {
  columnName: string
  operator: string
  value: unknown
  value2?: unknown
}

export interface SortOrderRequest {
  columnName: string
  direction: 'ASC' | 'DESC'
}

export interface RecordFilterRequest {
  columnFilters: ColumnFilterRequest[]
  customWhere?: string
  sortOrders: SortOrderRequest[]
}

export interface RecordCreateRequest {
  data: Record<string, unknown>
}

export interface RecordCreateResponse {
  createdRecord: Record<string, unknown>
  columnTypes: Record<string, string>
  executionTimeMs: number
  query: string
}

export interface RecordUpdateRequest {
  data: Record<string, unknown>
  whereConditions: Record<string, unknown>
}

export interface RecordUpdateResponse {
  updatedRecords: number
  executionTimeMs: number
  query: string
}

export interface RecordDeleteRequest {
  whereConditions: Record<string, unknown>
  skipReferentialIntegrityCheck: boolean
}

export interface RecordDeleteResponse {
  deletedRecords: number
  executionTimeMs: number
  query: string
}

// Database Connection Types
export type DatabaseType = 'MYSQL' | 'MARIADB' | 'POSTGRESQL' | 'H2'

export interface DatabaseRequest {
  name: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  username: string
  password: string
  connectionParams?: string
  active: boolean
}

export interface DatabaseResponse {
  id: number
  name: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  username: string
  connectionParams?: string
  active: boolean
  lastTestedAt?: string
  testResult?: boolean
  createdAt: string
  updatedAt: string
}

export interface ConnectionTestResponse {
  connected: boolean
  message?: string
  errorDetails?: string
  responseTimeMs?: number
}

// Schema Management Types
export type SchemaUpdateOperation = 'READ_SCHEMA' | 'REFRESH_SCHEMA' | 'IMPORT_SCHEMA' | 'EXPORT_SCHEMA'

// Permission Management Types
export type PermissionType = 'READ' | 'WRITE' | 'DELETE'
export type PermissionScope = 'TABLE' | 'COLUMN'

export interface PermissionExportOptions {
  description?: string
}

export interface PermissionImportOptions {
  importUsers?: boolean
  importTemplates?: boolean
  clearExistingPermissions?: boolean
  skipDuplicates?: boolean
}

export interface PermissionValidationResult {
  valid: boolean
  message: string
  userCount: number
  templateCount: number
  totalPermissions: number
}

export interface PermissionImportResult {
  importedUsers: number
  importedTemplates: number
  importedPermissions: number
  updatedPermissions: number
  skippedDuplicates: number
  warnings: string[]
  errors: string[]
}

// Bulk Permission Types
export type BulkPermissionScope = 'CONNECTION' | 'SCHEMA' | 'TABLE'
export type BulkPermissionType = 'READ' | 'WRITE' | 'DELETE'

export interface BulkPermissionRequest {
  scope: BulkPermissionScope
  permissionTypes: BulkPermissionType[]
  userEmails: string[]
  schemaNames?: string[]
  tableNames?: string[]
  description?: string
}

export interface BulkPermissionResponse {
  processedUsers: number
  processedTables: number
  createdPermissions: number
  updatedPermissions: number
  skippedExisting: number
  errors: string[]
}

export interface SchemaUpdateLogResponse {
  id: number
  connectionId: number
  operation: SchemaUpdateOperation
  userEmail: string
  executionTimeMs: number
  success: boolean
  errorMessage?: string
  tablesCount?: number
  columnsCount?: number
  details?: string
  createdAt: string
}

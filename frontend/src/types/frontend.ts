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

// Frontend types - optimized for UI state management

export interface User {
  email: string
  role: 'USER' | 'ADMIN'
}

export interface AuthState {
  isAuthenticated: boolean
  user: User | null
  accessToken: string | null
  refreshToken: string | null
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface PendingUser {
  id: number
  email: string
  registeredAt: Date
  isPending?: boolean
}

export interface AdminAction {
  type: 'approve' | 'reject'
  userId: number
  email: string
}

// Email First Registration Flow
export interface RegisterEmailCredentials {
  email: string
  language?: string
}

export interface RegisterEmailResult {
  email: string
}

export interface RegisterUserCredentials {
  token: string
  email: string
  password: string
  confirmPassword: string
  language?: string
}

export interface RegisterUserResult {
  userId: number
  email: string
}

// Data Access & Display Types
export interface AccessibleTable {
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
  columns: AccessibleColumn[]
}

export interface AccessibleColumn {
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

export interface SchemaMetadata {
  connectionId: number
  databaseName: string
  schemas: string[]
  tables: TableMetadata[]
  lastUpdatedAt: Date
}

export interface TableMetadata {
  schema: string
  tableName: string
  tableType: string
  comment?: string
  columns: ColumnMetadata[]
}

export interface ColumnMetadata {
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

export interface TableRecord {
  [columnName: string]: unknown
}

export interface RecordQueryData {
  records: TableRecord[]
  accessibleColumns: AccessibleColumn[]
  totalRecords: number
  currentPage: number
  pageSize: number
  totalPages: number
  hasNextPage: boolean
  hasPreviousPage: boolean
  executionTimeMs: number
  query: string
}

export interface ColumnFilter {
  columnName: string
  operator: 'EQUALS' | 'NOT_EQUALS' | 'LIKE' | 'NOT_LIKE' | 'GREATER_THAN' | 'LESS_THAN' | 'GREATER_EQUALS' | 'LESS_EQUALS' | 'BETWEEN' | 'IN' | 'NOT_IN' | 'IS_NULL' | 'IS_NOT_NULL'
  value?: unknown
  value2?: unknown
}

export interface SortOrder {
  columnName: string
  direction: 'ASC' | 'DESC'
}

export interface RecordFilter {
  columnFilters: ColumnFilter[]
  customWhere?: string
  sortOrders: SortOrder[]
}

export interface RecordCreateData {
  data: Record<string, unknown>
}

export interface RecordCreateResponse {
  createdRecord: TableRecord
  columnTypes: Record<string, string>
  executionTimeMs: number
  query: string
}

export interface RecordUpdateData {
  updateData: Record<string, unknown>
  whereConditions: Record<string, unknown>
}

export interface RecordUpdateResponse {
  updatedRecords: number
  executionTimeMs: number
  query: string
}

export interface RecordDeleteData {
  whereConditions: Record<string, unknown>
}

export interface RecordDeleteResponse {
  deletedRecords: number
  executionTimeMs: number
  query: string
}

// Database Connection Types (Frontend)
export type DatabaseType = 'MYSQL' | 'MARIADB' | 'POSTGRESQL' | 'H2'

// Permission Management Types (Frontend)
export type PermissionType = 'READ' | 'WRITE' | 'DELETE'
export type PermissionScope = 'TABLE' | 'COLUMN'

export interface PermissionImportOptions {
  importUsers: boolean
  importTemplates: boolean
  clearExistingPermissions: boolean
  duplicateHandling: 'error' | 'skip' | 'overwrite'
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

// Bulk Permission Types (Frontend)
export type BulkPermissionScope = 'ALL_TABLES' | 'SCHEMA' | 'TABLE_LIST'
export type BulkPermissionType = 'read' | 'write' | 'delete'

export interface BulkPermissionOptions {
  scope: BulkPermissionScope
  permissionType: BulkPermissionType
  userEmails: string[]
  schemaNames?: string[]
  tableNames?: string[]
  includeSystemTables: boolean
  description: string
}

export interface BulkPermissionResult {
  processedUsers: number
  processedTables: number
  createdPermissions: number
  skippedExisting: number
  errors: string[]
}

export interface DatabaseForm {
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

export interface Database {
  id: number
  name: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  username: string
  connectionParams?: string
  active: boolean
  lastTestedAt?: Date
  testResult?: boolean
  createdAt: Date
  updatedAt: Date
}

export interface ConnectionTestResult {
  connected: boolean
  message?: string
  errorDetails?: string
  responseTimeMs?: number
}

// Schema Management Types (Frontend)
export type SchemaUpdateOperation = 'READ_SCHEMA' | 'REFRESH_SCHEMA' | 'IMPORT_SCHEMA' | 'EXPORT_SCHEMA'

export interface SchemaUpdateLog {
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
  createdAt: Date
}

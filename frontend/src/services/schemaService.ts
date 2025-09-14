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

import apiClient from './apiClient'
import {API_ENDPOINTS} from '../config/config'
import type {
  ApiResponse,
  ColumnMetadataResponse,
  SchemaMetadataResponse,
  SchemaUpdateLogResponse,
  TableMetadataResponse
} from '../types/api'
import type {ColumnMetadata, SchemaMetadata, SchemaUpdateLog, TableMetadata} from '../types/frontend'

class SchemaService {

  async getSchema(connectionId: number): Promise<SchemaMetadata | null> {
    try {
      const response = await apiClient.get<ApiResponse<SchemaMetadataResponse>>(
        API_ENDPOINTS.SCHEMA.GET(connectionId)
      )

      // 204 No Content - キャッシュなし
      if (response.status === 204) {
        return null
      }

      if (!response.data.ok || !response.data.data) {
        throw new Error('Failed to get schema metadata')
      }

      return this.convertToFrontendSchema(response.data.data)
    } catch (error: unknown) {
      // 204は正常状態なので、他のHTTPエラーのみをthrow
      if ((error as { response?: { status: number } }).response?.status === 204) {
        return null
      }
      throw error
    }
  }

  async refreshSchema(connectionId: number): Promise<SchemaMetadata> {
    const response = await apiClient.post<ApiResponse<SchemaMetadataResponse>>(
      API_ENDPOINTS.SCHEMA.REFRESH(connectionId)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to refresh schema metadata')
    }

    return this.convertToFrontendSchema(response.data.data)
  }

  async getOperationHistory(connectionId: number): Promise<SchemaUpdateLog[]> {
    const response = await apiClient.get<ApiResponse<SchemaUpdateLogResponse[]>>(
      API_ENDPOINTS.SCHEMA.HISTORY(connectionId)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to get operation history')
    }

    return response.data.data.map(this.convertToFrontendLog)
  }

  async getFailedOperations(connectionId: number): Promise<SchemaUpdateLog[]> {
    const response = await apiClient.get<ApiResponse<SchemaUpdateLogResponse[]>>(
      API_ENDPOINTS.SCHEMA.FAILURES(connectionId)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to get failed operations')
    }

    return response.data.data.map(this.convertToFrontendLog)
  }

  // Type conversion methods (API → Frontend)
  private convertToFrontendSchema(apiSchema: SchemaMetadataResponse): SchemaMetadata {
    return {
      connectionId: apiSchema.connectionId,
      databaseName: apiSchema.databaseName,
      schemas: apiSchema.schemas,
      tables: apiSchema.tables.map(this.convertToFrontendTable),
      lastUpdatedAt: new Date(apiSchema.lastUpdatedAt)
    }
  }

  private convertToFrontendTable = (apiTable: TableMetadataResponse): TableMetadata => {
    return {
      schema: apiTable.schema,
      tableName: apiTable.tableName,
      tableType: apiTable.tableType,
      comment: apiTable.comment,
      columns: apiTable.columns.map(this.convertToFrontendColumn)
    }
  }

  private convertToFrontendColumn = (apiColumn: ColumnMetadataResponse): ColumnMetadata => {
    return {
      columnName: apiColumn.columnName,
      dataType: apiColumn.dataType,
      columnSize: apiColumn.columnSize,
      decimalDigits: apiColumn.decimalDigits,
      nullable: apiColumn.nullable,
      defaultValue: apiColumn.defaultValue,
      comment: apiColumn.comment,
      primaryKey: apiColumn.primaryKey,
      autoIncrement: apiColumn.autoIncrement,
      ordinalPosition: apiColumn.ordinalPosition
    }
  }

  private convertToFrontendLog = (apiLog: SchemaUpdateLogResponse): SchemaUpdateLog => {
    return {
      id: apiLog.id,
      connectionId: apiLog.connectionId,
      operation: apiLog.operation,
      userEmail: apiLog.userEmail,
      executionTimeMs: apiLog.executionTimeMs,
      success: apiLog.success,
      errorMessage: apiLog.errorMessage,
      tablesCount: apiLog.tablesCount,
      columnsCount: apiLog.columnsCount,
      details: apiLog.details,
      createdAt: new Date(apiLog.createdAt)
    }
  }
}

export const schemaService = new SchemaService()

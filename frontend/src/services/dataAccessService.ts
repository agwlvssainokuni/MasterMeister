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
  AccessibleTableResult,
  ApiResponse,
  RecordCreateRequest,
  RecordCreateResult,
  RecordDeleteRequest,
  RecordDeleteResult,
  RecordFilterRequest,
  RecordQueryResult,
  RecordUpdateRequest,
  RecordUpdateResult,
  TableMetadataResult
} from '../types/api'
import type {
  AccessibleTable,
  RecordCreateData,
  RecordCreateResponse,
  RecordDeleteData,
  RecordDeleteResponse,
  RecordFilter,
  RecordQueryData,
  RecordUpdateData,
  RecordUpdateResponse,
  TableMetadata
} from '../types/frontend'

class DataAccessService {

  async getAccessibleTables(connectionId: number): Promise<AccessibleTable[]> {
    const response = await apiClient.get<ApiResponse<AccessibleTableResult[]>>(
      API_ENDPOINTS.DATA_ACCESS.ACCESSIBLE_TABLES(connectionId)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch accessible tables')
    }

    return response.data.data.map(this.convertToAccessibleTable)
  }

  async getTableMetadata(connectionId: number, schemaName: string, tableName: string): Promise<TableMetadata> {
    const response = await apiClient.get<ApiResponse<TableMetadataResult>>(
      API_ENDPOINTS.DATA_ACCESS.TABLE_METADATA(connectionId, schemaName, tableName)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch table metadata')
    }

    return this.convertToTableMetadata(response.data.data)
  }

  async getRecords(
    connectionId: number,
    schemaName: string,
    tableName: string,
    page: number = 0,
    size: number = 20,
    filter?: RecordFilter
  ): Promise<RecordQueryData> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString()
    })

    let requestBody: RecordFilterRequest | undefined
    if (filter) {
      requestBody = this.convertToRecordFilterRequest(filter)
    }

    const url = `${API_ENDPOINTS.DATA_ACCESS.TABLE_RECORDS(connectionId, schemaName, tableName)}?${params.toString()}`

    const response = requestBody
      ? await apiClient.post<ApiResponse<RecordQueryResult>>(url, requestBody)
      : await apiClient.get<ApiResponse<RecordQueryResult>>(url)

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch records')
    }

    return this.convertToRecordQueryData(response.data.data)
  }

  async createRecord(
    connectionId: number,
    schemaName: string,
    tableName: string,
    data: RecordCreateData
  ): Promise<RecordCreateResponse> {
    const requestBody: RecordCreateRequest = {
      data: data.data
    }

    const response = await apiClient.post<ApiResponse<RecordCreateResult>>(
      API_ENDPOINTS.DATA_ACCESS.RECORD_CREATE(connectionId, schemaName, tableName),
      requestBody
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to create record')
    }

    return this.convertToRecordCreateResponse(response.data.data)
  }

  async updateRecord(
    connectionId: number,
    schemaName: string,
    tableName: string,
    data: RecordUpdateData
  ): Promise<RecordUpdateResponse> {
    const requestBody: RecordUpdateRequest = {
      updateData: data.updateData,
      whereConditions: data.whereConditions
    }

    const response = await apiClient.put<ApiResponse<RecordUpdateResult>>(
      API_ENDPOINTS.DATA_ACCESS.RECORD_UPDATE(connectionId, schemaName, tableName),
      requestBody
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to update record')
    }

    return this.convertToRecordUpdateResponse(response.data.data)
  }

  async deleteRecord(
    connectionId: number,
    schemaName: string,
    tableName: string,
    data: RecordDeleteData
  ): Promise<RecordDeleteResponse> {
    const requestBody: RecordDeleteRequest = {
      whereConditions: data.whereConditions
    }

    const response = await apiClient.post<ApiResponse<RecordDeleteResult>>(
      API_ENDPOINTS.DATA_ACCESS.RECORD_DELETE(connectionId, schemaName, tableName),
      requestBody
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to delete record')
    }

    return this.convertToRecordDeleteResponse(response.data.data)
  }

  // Type conversion methods (API → Frontend)
  private convertToAccessibleTable(apiTable: AccessibleTableResult): AccessibleTable {
    return {
      connectionId: apiTable.connectionId,
      schemaName: apiTable.schemaName,
      tableName: apiTable.tableName,
      fullTableName: apiTable.fullTableName,
      tableType: apiTable.tableType,
      comment: apiTable.comment,
      permissions: apiTable.permissions,
      canRead: apiTable.canRead,
      canWrite: apiTable.canWrite,
      canDelete: apiTable.canDelete,
      canAdmin: apiTable.canAdmin,
      canModifyData: apiTable.canModifyData,
      canPerformCrud: apiTable.canPerformCrud
    }
  }

  private convertToTableMetadata(apiMetadata: TableMetadataResult): TableMetadata {
    return {
      schema: apiMetadata.schema,
      tableName: apiMetadata.tableName,
      tableType: apiMetadata.tableType,
      comment: apiMetadata.comment,
      columns: apiMetadata.columns.map(col => ({
        columnName: col.columnName,
        dataType: col.dataType,
        columnSize: col.columnSize,
        decimalDigits: col.decimalDigits,
        nullable: col.nullable,
        defaultValue: col.defaultValue,
        comment: col.comment,
        primaryKey: col.primaryKey,
        autoIncrement: col.autoIncrement,
        ordinalPosition: col.ordinalPosition
      }))
    }
  }

  private convertToRecordQueryData(apiData: RecordQueryResult): RecordQueryData {
    return {
      records: apiData.records,
      accessibleColumns: apiData.accessibleColumns.map(col => ({
        columnName: col.columnName,
        dataType: col.dataType,
        columnSize: col.columnSize,
        decimalDigits: col.decimalDigits,
        nullable: col.nullable,
        defaultValue: col.defaultValue,
        comment: col.comment,
        primaryKey: col.primaryKey,
        autoIncrement: col.autoIncrement,
        ordinalPosition: col.ordinalPosition
      })),
      totalRecords: apiData.totalRecords,
      currentPage: apiData.currentPage,
      pageSize: apiData.pageSize,
      totalPages: apiData.totalPages,
      hasNextPage: apiData.hasNextPage,
      hasPreviousPage: apiData.hasPreviousPage,
      executionTimeMs: apiData.executionTimeMs,
      query: apiData.query
    }
  }

  private convertToRecordFilterRequest(filter: RecordFilter): RecordFilterRequest {
    return {
      columnFilters: filter.columnFilters.map(cf => ({
        columnName: cf.columnName,
        operator: cf.operator,
        value: cf.value,
        value2: cf.value2
      })),
      customWhere: filter.customWhere,
      sortOrders: filter.sortOrders.map(so => ({
        columnName: so.columnName,
        direction: so.direction
      }))
    }
  }

  private convertToRecordCreateResponse(apiResult: RecordCreateResult): RecordCreateResponse {
    return {
      createdRecord: apiResult.createdRecord,
      columnTypes: apiResult.columnTypes,
      executionTimeMs: apiResult.executionTimeMs,
      query: apiResult.query
    }
  }

  private convertToRecordUpdateResponse(apiResult: RecordUpdateResult): RecordUpdateResponse {
    return {
      updatedRecords: apiResult.updatedRecords,
      executionTimeMs: apiResult.executionTimeMs,
      query: apiResult.query
    }
  }

  private convertToRecordDeleteResponse(apiResult: RecordDeleteResult): RecordDeleteResponse {
    return {
      deletedRecords: apiResult.deletedRecords,
      executionTimeMs: apiResult.executionTimeMs,
      query: apiResult.query
    }
  }
}

export const dataAccessService = new DataAccessService()

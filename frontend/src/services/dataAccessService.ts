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
  AccessibleColumnResponse,
  AccessibleTableResponse,
  ApiResponse,
  DatabaseResponse,
  RecordCreateRequest,
  RecordCreateResponse as ApiRecordCreateResponse,
  RecordDeleteRequest,
  RecordDeleteResponse as ApiRecordDeleteResponse,
  RecordFilterRequest,
  RecordQueryResponse,
  RecordUpdateRequest,
  RecordUpdateResponse as ApiRecordUpdateResponse
} from '../types/api'
import type {
  AccessibleColumn,
  AccessibleTable,
  Database,
  RecordCreateData,
  RecordCreateResponse,
  RecordDeleteData,
  RecordDeleteResponse,
  RecordFilter,
  RecordQueryData,
  RecordUpdateData,
  RecordUpdateResponse
} from '../types/frontend'

class DataAccessService {

  async getDatabases(): Promise<Database[]> {
    const response = await apiClient.get<ApiResponse<DatabaseResponse[]>>(
      API_ENDPOINTS.DATA_ACCESS.DATABASES
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch databases')
    }

    return response.data.data.map(this.convertToFrontendDatabase)
  }

  async getAccessibleTables(connectionId: number): Promise<AccessibleTable[]> {
    const response = await apiClient.get<ApiResponse<AccessibleTableResponse[]>>(
      API_ENDPOINTS.DATA_ACCESS.ACCESSIBLE_TABLES(connectionId)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch accessible tables')
    }

    return response.data.data.map(this.convertToAccessibleTable)
  }

  async getTableDetails(connectionId: number, schemaName: string, tableName: string): Promise<AccessibleTable> {
    const response = await apiClient.get<ApiResponse<AccessibleTableResponse>>(
      API_ENDPOINTS.DATA_ACCESS.TABLE_DETAILS(connectionId, schemaName, tableName)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch table metadata')
    }

    return this.convertToAccessibleTable(response.data.data)
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

    const response = requestBody
      ? await apiClient.post<ApiResponse<RecordQueryResponse>>(
        `${API_ENDPOINTS.DATA_ACCESS.TABLE_RECORDS_FILTER(connectionId, schemaName, tableName)}?${params.toString()}`,
        requestBody
      )
      : await apiClient.get<ApiResponse<RecordQueryResponse>>(
        `${API_ENDPOINTS.DATA_ACCESS.TABLE_RECORDS(connectionId, schemaName, tableName)}?${params.toString()}`
      )

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

    const response = await apiClient.post<ApiResponse<ApiRecordCreateResponse>>(
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
      data: data.updateData,
      whereConditions: data.whereConditions
    }

    const response = await apiClient.put<ApiResponse<ApiRecordUpdateResponse>>(
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
      whereConditions: data.whereConditions,
      skipReferentialIntegrityCheck: false
    }

    const response = await apiClient.post<ApiResponse<ApiRecordDeleteResponse>>(
      API_ENDPOINTS.DATA_ACCESS.RECORD_DELETE(connectionId, schemaName, tableName),
      requestBody
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to delete record')
    }

    return this.convertToRecordDeleteResponse(response.data.data)
  }

  // Type conversion methods (API → Frontend)
  private convertToAccessibleTable(apiTable: AccessibleTableResponse): AccessibleTable {
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
      canPerformCrud: apiTable.canPerformCrud,
      columns: apiTable.columns?.map(col => this.convertToAccessibleColumn(col))
    }
  }

  private convertToAccessibleColumn = (apiColumn: AccessibleColumnResponse): AccessibleColumn => {
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
      ordinalPosition: apiColumn.ordinalPosition,
      permissions: apiColumn.permissions,
      canRead: apiColumn.canRead,
      canWrite: apiColumn.canWrite,
      canDelete: apiColumn.canDelete,
      canAdmin: apiColumn.canAdmin
    }
  }

  private convertToRecordQueryData(apiData: RecordQueryResponse): RecordQueryData {
    return {
      records: apiData.records,
      accessibleColumns: apiData.accessibleColumns.map(this.convertToAccessibleColumn),
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

  private convertToRecordCreateResponse(apiResult: ApiRecordCreateResponse): RecordCreateResponse {
    return {
      createdRecord: apiResult.createdRecord,
      columnTypes: apiResult.columnTypes,
      executionTimeMs: apiResult.executionTimeMs,
      query: apiResult.query
    }
  }

  private convertToRecordUpdateResponse(apiResult: ApiRecordUpdateResponse): RecordUpdateResponse {
    return {
      updatedRecords: apiResult.updatedRecords,
      executionTimeMs: apiResult.executionTimeMs,
      query: apiResult.query
    }
  }

  private convertToRecordDeleteResponse(apiResult: ApiRecordDeleteResponse): RecordDeleteResponse {
    return {
      deletedRecords: apiResult.deletedRecords,
      executionTimeMs: apiResult.executionTimeMs,
      query: apiResult.query
    }
  }

  private convertToFrontendDatabase(apiDatabase: DatabaseResponse): Database {
    return {
      id: apiDatabase.id,
      name: apiDatabase.name,
      dbType: apiDatabase.dbType,
      host: apiDatabase.host,
      port: apiDatabase.port,
      databaseName: apiDatabase.databaseName,
      username: apiDatabase.username,
      connectionParams: apiDatabase.connectionParams || '',
      active: apiDatabase.active,
      lastTestedAt: apiDatabase.lastTestedAt ? new Date(apiDatabase.lastTestedAt) : undefined,
      testResult: apiDatabase.testResult ?? undefined,
      createdAt: new Date(apiDatabase.createdAt),
      updatedAt: new Date(apiDatabase.updatedAt)
    }
  }
}

export const dataAccessService = new DataAccessService()

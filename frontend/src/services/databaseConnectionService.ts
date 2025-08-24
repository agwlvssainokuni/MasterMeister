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
import { API_ENDPOINTS } from '../config/config'
import type {
  ApiResponse,
  DatabaseConnectionRequest,
  DatabaseConnectionResult,
  ConnectionTestResult as ApiConnectionTestResult
} from '../types/api'
import type {
  DatabaseConnection,
  DatabaseConnectionForm,
  ConnectionTestResult
} from '../types/frontend'

class DatabaseConnectionService {

  async getAllConnections(): Promise<DatabaseConnection[]> {
    const response = await apiClient.get<ApiResponse<DatabaseConnectionResult[]>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.LIST
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch database connections')
    }

    return response.data.data.map(this.convertToFrontendConnection)
  }

  async getConnection(id: number): Promise<DatabaseConnection> {
    const response = await apiClient.get<ApiResponse<DatabaseConnectionResult>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.GET(id)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to fetch database connection')
    }

    return this.convertToFrontendConnection(response.data.data)
  }

  async createConnection(connectionForm: DatabaseConnectionForm): Promise<DatabaseConnection> {
    const requestBody: DatabaseConnectionRequest = this.convertToApiRequest(connectionForm)

    const response = await apiClient.post<ApiResponse<DatabaseConnectionResult>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.CREATE,
      requestBody
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to create database connection')
    }

    return this.convertToFrontendConnection(response.data.data)
  }

  async updateConnection(id: number, connectionForm: DatabaseConnectionForm): Promise<DatabaseConnection> {
    const requestBody: DatabaseConnectionRequest = this.convertToApiRequest(connectionForm)

    const response = await apiClient.put<ApiResponse<DatabaseConnectionResult>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.UPDATE(id),
      requestBody
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to update database connection')
    }

    return this.convertToFrontendConnection(response.data.data)
  }

  async deleteConnection(id: number): Promise<void> {
    const response = await apiClient.delete<ApiResponse<void>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.DELETE(id)
    )

    if (!response.data.ok) {
      throw new Error('Failed to delete database connection')
    }
  }

  async testConnection(id: number): Promise<ConnectionTestResult> {
    const response = await apiClient.post<ApiResponse<ApiConnectionTestResult>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.TEST(id)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to test database connection')
    }

    return this.convertToFrontendTestResult(response.data.data)
  }

  async activateConnection(id: number): Promise<DatabaseConnection> {
    const response = await apiClient.post<ApiResponse<DatabaseConnectionResult>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.ACTIVATE(id)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to activate database connection')
    }

    return this.convertToFrontendConnection(response.data.data)
  }

  async deactivateConnection(id: number): Promise<DatabaseConnection> {
    const response = await apiClient.post<ApiResponse<DatabaseConnectionResult>>(
      API_ENDPOINTS.DATABASE_CONNECTIONS.DEACTIVATE(id)
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to deactivate database connection')
    }

    return this.convertToFrontendConnection(response.data.data)
  }

  // Type conversion methods (API → Frontend)
  private convertToFrontendConnection(apiConnection: DatabaseConnectionResult): DatabaseConnection {
    return {
      id: apiConnection.id,
      name: apiConnection.name,
      dbType: apiConnection.dbType,
      host: apiConnection.host,
      port: apiConnection.port,
      databaseName: apiConnection.databaseName,
      username: apiConnection.username,
      connectionParams: apiConnection.connectionParams,
      active: apiConnection.active,
      lastTestedAt: apiConnection.lastTestedAt ? new Date(apiConnection.lastTestedAt) : undefined,
      testResult: apiConnection.testResult,
      createdAt: new Date(apiConnection.createdAt),
      updatedAt: new Date(apiConnection.updatedAt)
    }
  }

  private convertToFrontendTestResult(apiResult: ApiConnectionTestResult): ConnectionTestResult {
    return {
      connected: apiResult.connected,
      message: apiResult.message,
      errorDetails: apiResult.errorDetails,
      responseTimeMs: apiResult.responseTimeMs
    }
  }

  // Type conversion methods (Frontend → API)
  private convertToApiRequest(connectionForm: DatabaseConnectionForm): DatabaseConnectionRequest {
    return {
      name: connectionForm.name,
      dbType: connectionForm.dbType,
      host: connectionForm.host,
      port: connectionForm.port,
      databaseName: connectionForm.databaseName,
      username: connectionForm.username,
      password: connectionForm.password,
      connectionParams: connectionForm.connectionParams,
      active: connectionForm.active
    }
  }
}

export const databaseConnectionService = new DatabaseConnectionService()
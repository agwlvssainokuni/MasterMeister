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

import {API_ENDPOINTS} from '../config/config'
import apiClient from './apiClient'
import type {
  ApiResponse,
  BulkPermissionRequest as ApiBulkPermissionRequest,
  BulkPermissionResult as ApiBulkPermissionResult,
  PermissionImportResult as ApiPermissionImportResult,
  PermissionValidationResult as ApiPermissionValidationResult
} from '../types/api'
import type {
  BulkPermissionOptions,
  BulkPermissionResult,
  PermissionImportOptions,
  PermissionImportResult,
  PermissionValidationResult
} from '../types/frontend'

export class PermissionService {
  async exportPermissions(connectionId: number, description?: string): Promise<Blob> {
    const url = API_ENDPOINTS.PERMISSIONS.EXPORT(connectionId)
    const params = description ? {description} : {}

    const response = await apiClient.get(url, {
      params,
      responseType: 'blob'
    })

    return response.data as Blob
  }

  async importPermissions(
    connectionId: number,
    file: File,
    options: PermissionImportOptions
  ): Promise<PermissionImportResult> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('importUsers', options.importUsers.toString())
    formData.append('importTemplates', options.importTemplates.toString())
    formData.append('clearExistingPermissions', options.clearExistingPermissions.toString())
    formData.append('skipDuplicates', options.skipDuplicates.toString())

    const response = await apiClient.post<ApiResponse<ApiPermissionImportResult>>(
      API_ENDPOINTS.PERMISSIONS.IMPORT(connectionId),
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to import permissions')
    }

    return this.convertToFrontendImportResult(response.data.data)
  }

  async validatePermissionYaml(
    connectionId: number,
    file: File
  ): Promise<PermissionValidationResult> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post<ApiResponse<ApiPermissionValidationResult>>(
      API_ENDPOINTS.PERMISSIONS.VALIDATE(connectionId),
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to validate YAML')
    }

    return this.convertToFrontendValidationResult(response.data.data)
  }

  private convertToFrontendImportResult(apiResult: ApiPermissionImportResult): PermissionImportResult {
    return {
      importedUsers: apiResult.importedUsers,
      importedTemplates: apiResult.importedTemplates,
      importedPermissions: apiResult.importedPermissions,
      skippedDuplicates: apiResult.skippedDuplicates,
      errors: apiResult.errors
    }
  }

  private convertToFrontendValidationResult(apiResult: ApiPermissionValidationResult): PermissionValidationResult {
    return {
      valid: apiResult.valid,
      message: apiResult.message,
      userCount: apiResult.userCount,
      templateCount: apiResult.templateCount,
      totalPermissions: apiResult.totalPermissions
    }
  }

  async bulkGrantPermissions(
    connectionId: number,
    options: BulkPermissionOptions
  ): Promise<BulkPermissionResult> {
    // Convert frontend type to API type
    const apiRequest: ApiBulkPermissionRequest = {
      scope: options.scope.toUpperCase() as ApiBulkPermissionRequest['scope'],
      permissionType: options.permissionType.toUpperCase() as ApiBulkPermissionRequest['permissionType'],
      userEmails: options.userEmails,
      schemaNames: options.schemaNames,
      tableNames: options.tableNames,
      includeSystemTables: options.includeSystemTables,
      description: options.description
    }

    const response = await apiClient.post<ApiResponse<ApiBulkPermissionResult>>(
      API_ENDPOINTS.PERMISSIONS.BULK_GRANT(connectionId),
      apiRequest
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error('Failed to bulk grant permissions')
    }

    return this.convertToFrontendBulkResult(response.data.data)
  }

  private convertToFrontendBulkResult(apiResult: ApiBulkPermissionResult): BulkPermissionResult {
    return {
      processedUsers: apiResult.processedUsers,
      processedTables: apiResult.processedTables,
      createdPermissions: apiResult.createdPermissions,
      skippedExisting: apiResult.skippedExisting,
      errors: apiResult.errors
    }
  }
}

export const permissionService = new PermissionService()

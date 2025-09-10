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

import React, {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {AdminLayout} from './layouts/AdminLayout'
import {DatabaseSelectorView} from '../components/DatabaseSelectorView'
import {PermissionManagementView} from '../components/PermissionManagementView'
import {useNotification} from '../contexts/NotificationContext'
import {databaseService} from '../services/databaseService'
import {permissionService} from '../services/permissionService'
import type {
  BulkPermissionOptions,
  BulkPermissionResult,
  Database,
  PermissionImportOptions,
  PermissionImportResult,
  PermissionValidationResult
} from '../types/frontend'

export const PermissionManagementPage: React.FC = () => {
  const {t} = useTranslation()
  const {showSuccess, showError} = useNotification()

  const [connections, setConnections] = useState<Database[]>([])
  const [selectedConnection, setSelectedConnection] = useState<Database | null>(null)
  const [loading, setLoading] = useState(false)
  const [connectionsLoading, setConnectionsLoading] = useState(true)

  const loadConnections = React.useCallback(async () => {
    try {
      setConnectionsLoading(true)
      const allConnections = await databaseService.getAllConnections()
      const activeConnections = allConnections.filter(conn => conn.active)
      setConnections(activeConnections)
    } catch (error) {
      console.error('Failed to load connections:', error)
      showError(t('permissions.messages.loadConnectionsError'))
    } finally {
      setConnectionsLoading(false)
    }
  }, [showError, t])

  useEffect(() => {
    loadConnections()
  }, [loadConnections])

  const handleExportPermissions = async (connectionId: number, description?: string) => {
    try {
      setLoading(true)
      const blob = await permissionService.exportPermissions(connectionId, description)

      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `permissions-connection-${connectionId}.yml`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)

      const connection = connections.find(c => c.id === connectionId)
      showSuccess(
        t('permissions.messages.exportSuccess', {
          connection: connection?.name || `ID ${connectionId}`
        })
      )
    } catch (error) {
      console.error('Export failed:', error)
      showError(t('permissions.messages.exportError'))
    } finally {
      setLoading(false)
    }
  }

  const handleImportPermissions = async (
    connectionId: number,
    file: File,
    options: PermissionImportOptions
  ): Promise<PermissionImportResult> => {
    try {
      setLoading(true)
      const result = await permissionService.importPermissions(connectionId, file, options)

      const connection = connections.find(c => c.id === connectionId)
      showSuccess(
        t('permissions.messages.importSuccess', {
          connection: connection?.name || `ID ${connectionId}`,
          users: result.importedUsers,
          templates: result.importedTemplates,
          permissions: result.importedPermissions
        })
      )

      return result
    } catch (error) {
      console.error('Import failed:', error)
      showError(t('permissions.messages.importError'))
      throw error
    } finally {
      setLoading(false)
    }
  }

  const handleValidateYaml = async (
    connectionId: number,
    file: File
  ): Promise<PermissionValidationResult> => {
    try {
      setLoading(true)
      const result = await permissionService.validatePermissionYaml(connectionId, file)

      if (result.valid) {
        showSuccess(t('permissions.messages.validateSuccess'))
      } else {
        showError(
          t('permissions.messages.validateError', {message: result.message})
        )
      }

      return result
    } catch (error) {
      console.error('Validation failed:', error)
      showError(t('permissions.messages.validateError'))
      throw error
    } finally {
      setLoading(false)
    }
  }

  const handleBulkGrantPermissions = async (
    connectionId: number,
    options: BulkPermissionOptions
  ): Promise<BulkPermissionResult> => {
    try {
      setLoading(true)
      const result = await permissionService.bulkGrantPermissions(connectionId, options)

      const connection = connections.find(c => c.id === connectionId)
      showSuccess(
        t('permissions.messages.bulkGrantSuccess', {
          connection: connection?.name || `ID ${connectionId}`,
          created: result.createdPermissions,
          skipped: result.skippedExisting,
          users: result.processedUsers,
          tables: result.processedTables
        })
      )

      return result
    } catch (error) {
      console.error('Bulk grant failed:', error)
      showError(t('permissions.messages.bulkGrantError'))
      throw error
    } finally {
      setLoading(false)
    }
  }

  return (
    <AdminLayout
      title={t('permissions.title')}
      description={t('permissions.description')}
      className="permission-page"
    >
      {connectionsLoading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>{t('common.loading')}</p>
        </div>
      ) : (
        <>
          <DatabaseSelectorView
            connections={connections}
            selectedConnection={selectedConnection}
            onConnectionSelect={setSelectedConnection}
            i18nPrefix="permissions"
          />
          {selectedConnection && (
            <div className="permission-content">
              <PermissionManagementView
                connection={selectedConnection}
                loading={loading}
                onExport={handleExportPermissions}
                onImport={handleImportPermissions}
                onValidate={handleValidateYaml}
                onBulkGrant={handleBulkGrantPermissions}
              />
            </div>
          )}
        </>
      )}
    </AdminLayout>
  )
}

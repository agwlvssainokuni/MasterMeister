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

import React, { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { AdminLayout } from '../components/layouts/AdminLayout'
import { DatabaseConnectionList } from '../components/DatabaseConnectionList'
import { DatabaseConnectionForm } from '../components/DatabaseConnectionForm'
import { useNotification } from '../contexts/NotificationContext'
import { databaseConnectionService } from '../services/databaseConnectionService'
import type { DatabaseConnection, DatabaseConnectionForm as ConnectionForm } from '../types/frontend'

export const DatabaseConnectionsPage: React.FC = () => {
  const { t } = useTranslation()
  const { addNotification } = useNotification()
  
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingConnection, setEditingConnection] = useState<DatabaseConnection | null>(null)

  useEffect(() => {
    loadConnections()
  }, [])

  const loadConnections = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await databaseConnectionService.getAllConnections()
      setConnections(data)
    } catch (err) {
      console.error('Error loading database connections:', err)
      setError(err instanceof Error ? err.message : 'Failed to load database connections')
    } finally {
      setLoading(false)
    }
  }

  const handleCreateConnection = () => {
    setEditingConnection(null)
    setShowForm(true)
  }

  const handleEditConnection = (connection: DatabaseConnection) => {
    setEditingConnection(connection)
    setShowForm(true)
  }

  const handleFormSubmit = async (formData: ConnectionForm) => {
    try {
      if (editingConnection) {
        await databaseConnectionService.updateConnection(editingConnection.id, formData)
        addNotification({
          type: 'success',
          message: t('databaseConnections.messages.updateSuccess', { name: formData.name })
        })
      } else {
        await databaseConnectionService.createConnection(formData)
        addNotification({
          type: 'success',
          message: t('databaseConnections.messages.createSuccess', { name: formData.name })
        })
      }
      
      setShowForm(false)
      setEditingConnection(null)
      await loadConnections()
    } catch (err) {
      console.error('Error saving database connection:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databaseConnections.messages.saveError')
      })
    }
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setEditingConnection(null)
  }

  const handleDeleteConnection = async (connection: DatabaseConnection) => {
    if (!window.confirm(t('databaseConnections.confirmDelete', { name: connection.name }))) {
      return
    }

    try {
      await databaseConnectionService.deleteConnection(connection.id)
      addNotification({
        type: 'success',
        message: t('databaseConnections.messages.deleteSuccess', { name: connection.name })
      })
      await loadConnections()
    } catch (err) {
      console.error('Error deleting database connection:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databaseConnections.messages.deleteError')
      })
    }
  }

  const handleTestConnection = async (connection: DatabaseConnection) => {
    try {
      const result = await databaseConnectionService.testConnection(connection.id)
      
      if (result.connected) {
        addNotification({
          type: 'success',
          message: t('databaseConnections.messages.testSuccess', { 
            name: connection.name,
            responseTime: result.responseTimeMs 
          })
        })
      } else {
        addNotification({
          type: 'error',
          message: t('databaseConnections.messages.testFailed', {
            name: connection.name,
            error: result.message || result.errorDetails || 'Unknown error'
          })
        })
      }
      
      await loadConnections()
    } catch (err) {
      console.error('Error testing database connection:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databaseConnections.messages.testError')
      })
    }
  }

  const handleToggleActive = async (connection: DatabaseConnection) => {
    try {
      if (connection.active) {
        await databaseConnectionService.deactivateConnection(connection.id)
        addNotification({
          type: 'success',
          message: t('databaseConnections.messages.deactivateSuccess', { name: connection.name })
        })
      } else {
        await databaseConnectionService.activateConnection(connection.id)
        addNotification({
          type: 'success',
          message: t('databaseConnections.messages.activateSuccess', { name: connection.name })
        })
      }
      
      await loadConnections()
    } catch (err) {
      console.error('Error toggling connection status:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databaseConnections.messages.toggleError')
      })
    }
  }

  if (loading) {
    return (
      <AdminLayout title={t('databaseConnections.title')}>
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p>{t('common.loading')}</p>
        </div>
      </AdminLayout>
    )
  }

  if (error) {
    return (
      <AdminLayout title={t('databaseConnections.title')}>
        <div className="error-state">
          <p className="error-message">{error}</p>
          <button onClick={loadConnections} className="button button-primary">
            {t('common.retry')}
          </button>
        </div>
      </AdminLayout>
    )
  }

  return (
    <AdminLayout title={t('databaseConnections.title')}>
      <div className="page-section">
        <div className="section-header">
          <p className="section-description">{t('databaseConnections.description')}</p>
          <div className="section-actions">
            <button 
              className="button button-primary" 
              onClick={handleCreateConnection}
            >
              {t('databaseConnections.createConnection')}
            </button>
          </div>
        </div>

        {showForm ? (
          <DatabaseConnectionForm
            connection={editingConnection}
            onSubmit={handleFormSubmit}
            onCancel={handleFormCancel}
          />
        ) : (
          <DatabaseConnectionList
            connections={connections}
            onEdit={handleEditConnection}
            onDelete={handleDeleteConnection}
            onTest={handleTestConnection}
            onToggleActive={handleToggleActive}
          />
        )}
      </div>
    </AdminLayout>
  )
}
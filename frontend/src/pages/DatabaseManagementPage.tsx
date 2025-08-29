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
import {DatabaseListView} from '../components/DatabaseListView'
import {DatabaseFormView} from '../components/DatabaseFormView'
import {ConfirmDialog} from '../components/ConfirmDialog'
import {useNotification} from '../contexts/NotificationContext'
import {databaseService} from '../services/databaseService'
import type {Database, DatabaseForm as ConnectionForm} from '../types/frontend'

export const DatabaseManagementPage: React.FC = () => {
  const {t} = useTranslation()
  const {addNotification} = useNotification()

  const [connections, setConnections] = useState<Database[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingConnection, setEditingConnection] = useState<Database | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<{
    isOpen: boolean
    connection: Database | null
    loading: boolean
  }>({
    isOpen: false,
    connection: null,
    loading: false
  })

  useEffect(() => {
    loadConnections()
  }, [])

  const loadConnections = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await databaseService.getAllConnections()
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

  const handleEditConnection = (connection: Database) => {
    setEditingConnection(connection)
    setShowForm(true)
  }

  const handleFormSubmit = async (formData: ConnectionForm) => {
    try {
      if (editingConnection) {
        await databaseService.updateConnection(editingConnection.id, formData)
        addNotification({
          type: 'success',
          message: t('databases.messages.updateSuccess', {name: formData.name})
        })
      } else {
        await databaseService.createConnection(formData)
        addNotification({
          type: 'success',
          message: t('databases.messages.createSuccess', {name: formData.name})
        })
      }

      setShowForm(false)
      setEditingConnection(null)
      await loadConnections()
    } catch (err) {
      console.error('Error saving database connection:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databases.messages.saveError')
      })
    }
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setEditingConnection(null)
  }

  const handleDeleteConnection = (connection: Database) => {
    setDeleteConfirm({
      isOpen: true,
      connection,
      loading: false
    })
  }

  const handleConfirmDelete = async () => {
    if (!deleteConfirm.connection) return

    const connection = deleteConfirm.connection

    try {
      setDeleteConfirm(prev => ({...prev, loading: true}))

      await databaseService.deleteConnection(connection.id)

      addNotification({
        type: 'success',
        message: t('databases.messages.deleteSuccess', {name: connection.name})
      })

      await loadConnections()
      setDeleteConfirm({
        isOpen: false,
        connection: null,
        loading: false
      })
    } catch (err) {
      console.error('Error deleting database connection:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databases.messages.deleteError')
      })
      setDeleteConfirm(prev => ({...prev, loading: false}))
    }
  }

  const handleCancelDelete = () => {
    if (!deleteConfirm.loading) {
      setDeleteConfirm({
        isOpen: false,
        connection: null,
        loading: false
      })
    }
  }

  const handleTestConnection = async (connection: Database) => {
    try {
      const result = await databaseService.testConnection(connection.id)

      if (result.connected) {
        addNotification({
          type: 'success',
          message: t('databases.messages.testSuccess', {
            name: connection.name,
            responseTime: result.responseTimeMs
          })
        })
      } else {
        addNotification({
          type: 'error',
          message: t('databases.messages.testFailed', {
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
        message: err instanceof Error ? err.message : t('databases.messages.testError')
      })
    }
  }

  const handleToggleActive = async (connection: Database) => {
    try {
      if (connection.active) {
        await databaseService.deactivateConnection(connection.id)
        addNotification({
          type: 'success',
          message: t('databases.messages.deactivateSuccess', {name: connection.name})
        })
      } else {
        await databaseService.activateConnection(connection.id)
        addNotification({
          type: 'success',
          message: t('databases.messages.activateSuccess', {name: connection.name})
        })
      }

      await loadConnections()
    } catch (err) {
      console.error('Error toggling connection status:', err)
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('databases.messages.toggleError')
      })
    }
  }

  if (loading) {
    return (
      <AdminLayout
        title={t('databases.title')}
        description={t('databases.description')}
        className="database-page"
      >
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p>{t('common.loading')}</p>
        </div>
      </AdminLayout>
    )
  }

  if (error) {
    return (
      <AdminLayout title={t('databases.title')} description={t('databases.description')}
                   className="database-page">

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
    <AdminLayout
      title={t('databases.title')}
      description={t('databases.description')}
      className="database-connections-page"
    >
      <div className="section-header">
        <div className="section-actions">
          <button
            className="button button-primary"
            onClick={handleCreateConnection}
          >
            {t('databases.createConnection')}
          </button>
        </div>
      </div>

      <div className="content-spacing">
        {showForm ? (
          <DatabaseFormView
            connection={editingConnection}
            onSubmit={handleFormSubmit}
            onCancel={handleFormCancel}
          />
        ) : (
          <DatabaseListView
            connections={connections}
            onEdit={handleEditConnection}
            onDelete={handleDeleteConnection}
            onTest={handleTestConnection}
            onToggleActive={handleToggleActive}
          />
        )}
      </div>

      <ConfirmDialog
        isOpen={deleteConfirm.isOpen}
        onClose={handleCancelDelete}
        onConfirm={handleConfirmDelete}
        title={t('databases.confirmDelete.title')}
        message={t('databases.confirmDelete.message', {name: deleteConfirm.connection?.name || ''})}
        confirmText={t('databases.confirmDelete.confirm')}
        cancelText={t('databases.confirmDelete.cancel')}
        type="danger"
        loading={deleteConfirm.loading}
      />
    </AdminLayout>
  )
}

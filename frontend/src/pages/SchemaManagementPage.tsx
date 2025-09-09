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
import {SchemaMetadataView} from '../components/SchemaMetadataView'
import {SchemaOperationHistoryView} from '../components/SchemaOperationHistoryView'
import {useNotification} from '../contexts/NotificationContext'
import {databaseService} from '../services/databaseService'
import {schemaService} from '../services/schemaService'
import type {Database, SchemaMetadata, SchemaUpdateLog} from '../types/frontend'

export const SchemaManagementPage: React.FC = () => {
  const {t} = useTranslation()
  const {addNotification} = useNotification()

  const [connections, setConnections] = useState<Database[]>([])
  const [selectedConnection, setSelectedConnection] = useState<Database | null>(null)
  const [schema, setSchema] = useState<SchemaMetadata | null>(null)
  const [operationHistory, setOperationHistory] = useState<SchemaUpdateLog[]>([])

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'schema' | 'history'>('schema')

  useEffect(() => {
    loadConnections()
  }, [])

  const loadConnections = async () => {
    try {
      setLoading(true)
      const data = await databaseService.getAllConnections()
      // Only show active connections
      const activeConnections = data.filter(conn => conn.active)
      setConnections(activeConnections)
    } catch (err) {
      console.error('Error loading database connections:', err)
      setError(err instanceof Error ? err.message : 'Failed to load database connections')
    } finally {
      setLoading(false)
    }
  }

  const handleConnectionSelect = async (connection: Database) => {
    setSelectedConnection(connection)
    setSchema(null)
    setOperationHistory([])
    setError(null)

    await Promise.all([
      loadSchema(connection.id),
      loadOperationHistory(connection.id)
    ])
  }

  const loadSchema = async (connectionId: number) => {
    try {
      setLoading(true)
      setError(null)

      const schemaData = await schemaService.getSchema(connectionId)
      if (schemaData) {
        setSchema(schemaData)
      } else {
        // キャッシュなし（204）の場合は何もしない
        console.log('No cached schema found for connection ID:', connectionId)
      }
    } catch (err) {
      console.error('Error loading schema:', err)
      // 実際のエラーの場合は何もしない（エラーを表示しない）
    } finally {
      setLoading(false)
    }
  }

  const loadOperationHistory = async (connectionId: number) => {
    try {
      const history = await schemaService.getOperationHistory(connectionId)
      setOperationHistory(history)
    } catch (err) {
      console.error('Error loading operation history:', err)
    }
  }

  const handleRefreshSchema = async () => {
    if (!selectedConnection) return

    try {
      setLoading(true)
      setError(null)

      const schemaData = await schemaService.refreshSchema(selectedConnection.id)
      setSchema(schemaData)

      addNotification({
        type: 'success',
        message: t('schema.messages.refreshSuccess', {
          database: schemaData.databaseName,
          tablesCount: schemaData.tables.length
        })
      })

      await loadOperationHistory(selectedConnection.id)
    } catch (err) {
      console.error('Error refreshing schema:', err)
      setError(err instanceof Error ? err.message : 'Failed to refresh schema')
      addNotification({
        type: 'error',
        message: err instanceof Error ? err.message : t('schema.messages.refreshError')
      })
    } finally {
      setLoading(false)
    }
  }

  if (loading && connections.length === 0) {
    return (
      <AdminLayout
        title={t('schema.title')}
        description={t('schema.description')}
        className="schema-page"
      >
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p>{t('common.loading')}</p>
        </div>
      </AdminLayout>
    )
  }

  if (error && connections.length === 0) {
    return (
      <AdminLayout
        title={t('schema.title')}
        description={t('schema.description')}
        className="schema-page"
      >
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
      title={t('schema.title')}
      description={t('schema.description')}
      className="schema-page"
    >
      <DatabaseSelectorView
        connections={connections}
        selectedConnection={selectedConnection}
        onConnectionSelect={handleConnectionSelect}
        i18nPrefix="schema"
      />
      {selectedConnection && (
        <div className="schema-content">
          <div className="tabs">
            <ul className="tabs-list">
              <li className={`tab-item ${activeTab === 'schema' ? 'active' : ''}`}>
                <button
                  type="button"
                  className="tab-button"
                  onClick={() => setActiveTab('schema')}
                >
                  {t('schema.tabs.metadata')}
                </button>
              </li>
              <li className={`tab-item ${activeTab === 'history' ? 'active' : ''}`}>
                <button
                  type="button"
                  className="tab-button"
                  onClick={() => setActiveTab('history')}
                >
                  {t('schema.tabs.history')}
                </button>
              </li>
            </ul>
          </div>
          <div className="tab-content">
            {activeTab === 'schema' && (
              <SchemaMetadataView
                schema={schema}
                loading={loading}
                error={error}
                onRefreshSchema={handleRefreshSchema}
              />
            )}
            {activeTab === 'history' && (
              <SchemaOperationHistoryView
                connection={selectedConnection}
                operationHistory={operationHistory}
                onRefresh={() => loadOperationHistory(selectedConnection.id)}
              />
            )}
          </div>
        </div>
      )}
    </AdminLayout>
  )
}

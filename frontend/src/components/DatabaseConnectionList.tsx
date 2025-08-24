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

import React from 'react'
import { useTranslation } from 'react-i18next'
import type { DatabaseConnection } from '../types/frontend'

interface DatabaseConnectionListProps {
  connections: DatabaseConnection[]
  onEdit: (connection: DatabaseConnection) => void
  onDelete: (connection: DatabaseConnection) => void
  onTest: (connection: DatabaseConnection) => void
  onToggleActive: (connection: DatabaseConnection) => void
}

export const DatabaseConnectionList: React.FC<DatabaseConnectionListProps> = ({
  connections,
  onEdit,
  onDelete,
  onTest,
  onToggleActive
}) => {
  const { t } = useTranslation()

  const getStatusIcon = (connection: DatabaseConnection) => {
    if (!connection.active) return '⏸️'
    if (connection.testResult === true) return '✅'
    if (connection.testResult === false) return '❌'
    return '⏳'
  }

  const getStatusText = (connection: DatabaseConnection) => {
    if (!connection.active) return t('databaseConnections.status.inactive')
    if (connection.testResult === true) return t('databaseConnections.status.connected')
    if (connection.testResult === false) return t('databaseConnections.status.connectionFailed')
    return t('databaseConnections.status.untested')
  }

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  }

  const getDatabaseTypeLabel = (dbType: string) => {
    switch (dbType) {
      case 'MYSQL': return 'MySQL'
      case 'MARIADB': return 'MariaDB'
      case 'POSTGRESQL': return 'PostgreSQL'
      case 'H2': return 'H2'
      default: return dbType
    }
  }

  if (connections.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">🔗</div>
        <h3>{t('databaseConnections.noConnections')}</h3>
        <p>{t('databaseConnections.noConnectionsDescription')}</p>
      </div>
    )
  }

  return (
    <div className="connection-list">
      <div className="connection-grid">
        {connections.map(connection => (
          <div key={connection.id} className="connection-card">
            <div className="connection-header">
              <div className="connection-title">
                <h3>{connection.name}</h3>
                <span className="connection-type">
                  {getDatabaseTypeLabel(connection.dbType)}
                </span>
              </div>
              <div className="connection-status">
                <span className="status-icon">{getStatusIcon(connection)}</span>
                <span className="status-text">{getStatusText(connection)}</span>
              </div>
            </div>

            <div className="connection-details">
              <div className="detail-row">
                <span className="detail-label">{t('databaseConnections.fields.host')}</span>
                <span className="detail-value">{connection.host}:{connection.port}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">{t('databaseConnections.fields.database')}</span>
                <span className="detail-value">{connection.databaseName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">{t('databaseConnections.fields.username')}</span>
                <span className="detail-value">{connection.username}</span>
              </div>
              {connection.lastTestedAt && (
                <div className="detail-row">
                  <span className="detail-label">{t('databaseConnections.lastTested')}</span>
                  <span className="detail-value">{formatDate(connection.lastTestedAt)}</span>
                </div>
              )}
            </div>

            <div className="connection-actions">
              <button
                className="button button-sm button-secondary"
                onClick={() => onTest(connection)}
                title={t('databaseConnections.actions.test')}
              >
                🔍 {t('databaseConnections.actions.test')}
              </button>
              <button
                className={`button button-sm ${connection.active ? 'button-warning' : 'button-success'}`}
                onClick={() => onToggleActive(connection)}
                title={connection.active ? t('databaseConnections.actions.deactivate') : t('databaseConnections.actions.activate')}
              >
                {connection.active ? '⏸️' : '▶️'} 
                {connection.active ? t('databaseConnections.actions.deactivate') : t('databaseConnections.actions.activate')}
              </button>
              <button
                className="button button-sm button-primary"
                onClick={() => onEdit(connection)}
                title={t('databaseConnections.actions.edit')}
              >
                ✏️ {t('databaseConnections.actions.edit')}
              </button>
              <button
                className="button button-sm button-danger"
                onClick={() => onDelete(connection)}
                title={t('databaseConnections.actions.delete')}
              >
                🗑️ {t('databaseConnections.actions.delete')}
              </button>
            </div>

            <div className="connection-footer">
              <span className="timestamp">
                {t('databaseConnections.created')}: {formatDate(connection.createdAt)}
              </span>
              {connection.updatedAt && connection.updatedAt.getTime() !== connection.createdAt.getTime() && (
                <span className="timestamp">
                  {t('databaseConnections.updated')}: {formatDate(connection.updatedAt)}
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
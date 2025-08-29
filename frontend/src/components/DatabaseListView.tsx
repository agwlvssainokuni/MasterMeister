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
import {useTranslation} from 'react-i18next'
import {FaCheckCircle, FaClock, FaLink, FaPause, FaTimesCircle} from 'react-icons/fa'
import type {Database} from '../types/frontend'

interface DatabaseListViewProps {
  connections: Database[]
  onEdit: (connection: Database) => void
  onDelete: (connection: Database) => void
  onTest: (connection: Database) => void
  onToggleActive: (connection: Database) => void
}

export const DatabaseListView: React.FC<DatabaseListViewProps> = (
  {
    connections,
    onEdit,
    onDelete,
    onTest,
    onToggleActive,
  },
) => {
  const {t} = useTranslation()

  const getStatusIcon = (connection: Database) => {
    if (!connection.active) return <FaPause/>
    if (connection.testResult === true) return <FaCheckCircle/>
    if (connection.testResult === false) return <FaTimesCircle/>
    return <FaClock/>
  }

  const getStatusClass = (connection: Database) => {
    if (!connection.active) return 'status-warning'
    if (connection.testResult === true) return 'status-success'
    if (connection.testResult === false) return 'status-error'
    return 'status-info'
  }

  const getStatusText = (connection: Database) => {
    if (!connection.active) return t('databases.status.inactive')
    if (connection.testResult === true) return t('databases.status.connected')
    if (connection.testResult === false) return t('databases.status.connectionFailed')
    return t('databases.status.untested')
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
      case 'MYSQL':
        return 'MySQL'
      case 'MARIADB':
        return 'MariaDB'
      case 'POSTGRESQL':
        return 'PostgreSQL'
      case 'H2':
        return 'H2'
      default:
        return dbType
    }
  }

  if (connections.length === 0) {
    return (
      <div className="card">
        <div className="card-body empty-state-card">
          <div className="empty-state-icon"><FaLink/></div>
          <h3 className="empty-state-title">{t('databases.noConnections')}</h3>
          <p className="empty-state-description">
            {t('databases.noConnectionsDescription')}
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="card-grid">
      {connections.map(connection => (
        <div key={connection.id} className={`card card-status ${getStatusClass(connection)}`}>
          <div className="card-header">
            <div>
              <h3 className="card-title">{connection.name}</h3>
              <div className="card-subtitle-row">
                <p className="card-subtitle">
                  {getDatabaseTypeLabel(connection.dbType)}
                </p>
                <div className="status-display">
                  <span className="status-icon">{getStatusIcon(connection)}</span>
                  <span className="status-text">
                    {getStatusText(connection)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="card-body">
            <div className="card-details-grid">
              <div className="detail-row">
                <span className="detail-label">
                  {t('databases.fields.host')}
                </span>
                <span className="detail-value">{connection.host}:{connection.port}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">
                  {t('databases.fields.database')}
                </span>
                <span className="detail-value">{connection.databaseName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">
                  {t('databases.fields.username')}
                </span>
                <span className="detail-value">{connection.username}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">
                  {t('databases.lastTested')}
                </span>
                <span className="detail-value detail-value-small">
                  {connection.lastTestedAt ? formatDate(connection.lastTestedAt) : t('databases.status.untested')}
                </span>
              </div>
            </div>
          </div>

          <div className="card-footer">
            <div className="card-footer-timestamp">
              <div>{t('databases.created')}: {formatDate(connection.createdAt)}</div>
              {connection.updatedAt && connection.updatedAt.getTime() !== connection.createdAt.getTime() && (
                <div>{t('databases.updated')}: {formatDate(connection.updatedAt)}</div>
              )}
            </div>
            <div className="card-actions-grid">
              <button
                className="button button-sm button-secondary"
                onClick={() => onTest(connection)}
                title={t('databases.actions.test')}
              >
                {t('databases.actions.test')}
              </button>
              <button
                className={`button button-sm ${connection.active ? 'button-warning' : 'button-success'}`}
                onClick={() => onToggleActive(connection)}
                title={connection.active ? t('databases.actions.deactivate') : t('databases.actions.activate')}
              >
                {connection.active ? t('databases.actions.deactivate') : t('databases.actions.activate')}
              </button>
              <button
                className="button button-sm button-primary"
                onClick={() => onEdit(connection)}
                title={t('databases.actions.edit')}
              >
                {t('databases.actions.edit')}
              </button>
              <button
                className="button button-sm button-danger"
                onClick={() => onDelete(connection)}
                title={t('databases.actions.delete')}
              >
                {t('databases.actions.delete')}
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

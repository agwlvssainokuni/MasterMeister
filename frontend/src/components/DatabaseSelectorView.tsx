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
import type {Database} from '../types/frontend'

interface DatabaseSelectorViewProps {
  connections: Database[]
  selectedConnection: Database | null
  onConnectionSelect: (connection: Database) => void
  i18nPrefix: 'permissions' | 'schema'
  className?: string
}

export const DatabaseSelectorView: React.FC<DatabaseSelectorViewProps> = (
  {
    connections,
    selectedConnection,
    onConnectionSelect,
    i18nPrefix,
    className = ''
  }
) => {
  const {t} = useTranslation()

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
      <div className={`database-selector ${className}`}>
        <div className="card">
          <div className="card-body empty-state-card">
            <div className="empty-state-icon">🔗</div>
            <h3 className="empty-state-title">{t(`${i18nPrefix}.noActiveConnections`)}</h3>
            <p className="empty-state-description">{t(`${i18nPrefix}.noActiveConnectionsDescription`)}</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={`database-selector ${className}`}>
      <h3 className="selector-title">{t(`${i18nPrefix}.selectConnection`)}</h3>
      <p className="selector-description">{t(`${i18nPrefix}.selectConnectionDescription`)}</p>

      <div className="card-grid">
        {connections.map(connection => (
          <div
            key={connection.id}
            className={`card ${selectedConnection?.id === connection.id ? 'card-selected' : ''}`}
            onClick={() => onConnectionSelect(connection)}
          >
            <div className="card-header">
              <div>
                <h3 className="card-title">{connection.name}</h3>
                <div className="card-subtitle-row">
                  <p className="card-subtitle">
                    {getDatabaseTypeLabel(connection.dbType)}
                  </p>
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
              </div>
            </div>

            {selectedConnection?.id === connection.id && (
              <div className="card-footer">
                <div className="selected-indicator">
                  <span className="indicator-icon">✓</span>
                  <span className="indicator-text">{t(`${i18nPrefix}.connectionSelected`)}</span>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

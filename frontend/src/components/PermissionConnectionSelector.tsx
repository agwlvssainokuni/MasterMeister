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
import type {DatabaseConnection} from '../types/frontend'

interface PermissionConnectionSelectorProps {
  connections: DatabaseConnection[]
  selectedConnection: DatabaseConnection | null
  onConnectionSelect: (connection: DatabaseConnection) => void
}

export const PermissionConnectionSelector: React.FC<PermissionConnectionSelectorProps> = ({
                                                                                            connections,
                                                                                            selectedConnection,
                                                                                            onConnectionSelect
                                                                                          }) => {
  const {t} = useTranslation()

  if (connections.length === 0) {
    return (
      <div className="connection-selector-section">
        <h2>{t('permissions.selectConnection')}</h2>
        <div className="empty-connections-state">
          <div className="empty-icon">🔗</div>
          <h3>{t('permissions.noActiveConnections')}</h3>
          <p>{t('permissions.noActiveConnectionsDescription')}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="connection-selector-section">
      <h2>{t('permissions.selectConnection')}</h2>
      <p className="section-description">
        {t('permissions.selectConnectionDescription')}
      </p>

      <div className="connections-grid">
        {connections.map(connection => (
          <div
            key={connection.id}
            className={`connection-card ${selectedConnection?.id === connection.id ? 'selected' : ''}`}
            onClick={() => onConnectionSelect(connection)}
          >
            <div className="connection-header">
              <div className="connection-info">
                <h4 className="connection-name">{connection.name}</h4>
                <span className="connection-type">{connection.dbType}</span>
              </div>
              {selectedConnection?.id === connection.id && (
                <div className="selected-badge">
                  <span className="badge-icon">✓</span>
                  <span className="badge-text">{t('permissions.connectionSelected')}</span>
                </div>
              )}
            </div>

            <div className="connection-details">
              <p className="connection-address">
                {t('permissions.connectionInfo', {
                  host: connection.host,
                  port: connection.port,
                  type: connection.dbType
                })}
              </p>
              <p className="database-name">
                <strong>Database:</strong> {connection.databaseName}
              </p>
            </div>

            <div className="connection-status">
              <div className={`status-indicator ${connection.active ? 'active' : 'inactive'}`}>
                {connection.active ? t('databaseConnections.status.active') : t('databaseConnections.status.inactive')}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

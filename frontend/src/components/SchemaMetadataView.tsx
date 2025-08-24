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

import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { DatabaseConnection, SchemaMetadata, TableMetadata } from '../types/frontend'

interface SchemaMetadataViewProps {
  connection: DatabaseConnection
  schema: SchemaMetadata | null
  loading: boolean
  error: string | null
  onReadSchema: () => void
  onRefreshSchema: () => void
}

export const SchemaMetadataView: React.FC<SchemaMetadataViewProps> = ({
  connection,
  schema,
  loading,
  error,
  onReadSchema,
  onRefreshSchema
}) => {
  const { t } = useTranslation()
  const [expandedTables, setExpandedTables] = useState<Set<string>>(new Set())
  const [searchTerm, setSearchTerm] = useState('')

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  }

  const toggleTableExpansion = (tableKey: string) => {
    const newExpanded = new Set(expandedTables)
    if (newExpanded.has(tableKey)) {
      newExpanded.delete(tableKey)
    } else {
      newExpanded.add(tableKey)
    }
    setExpandedTables(newExpanded)
  }

  const getTableKey = (table: TableMetadata) => `${table.schema}.${table.tableName}`

  const filteredTables = schema?.tables.filter(table => 
    table.tableName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    table.schema.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (table.comment && table.comment.toLowerCase().includes(searchTerm.toLowerCase()))
  ) || []

  const groupedTables = filteredTables.reduce((groups, table) => {
    const schema = table.schema
    if (!groups[schema]) {
      groups[schema] = []
    }
    groups[schema].push(table)
    return groups
  }, {} as Record<string, TableMetadata[]>)

  return (
    <div className="schema-metadata-view">
      <div className="schema-header">
        <div className="schema-info">
          <h3>{connection.databaseName}</h3>
          <p className="schema-description">
            {t('schema.connectionInfo', { 
              host: connection.host,
              port: connection.port,
              type: connection.dbType
            })}
          </p>
        </div>
        
        <div className="schema-actions">
          <button
            className="button button-secondary"
            onClick={onReadSchema}
            disabled={loading}
          >
            {loading ? t('schema.reading') : t('schema.actions.read')}
          </button>
          <button
            className="button button-primary"
            onClick={onRefreshSchema}
            disabled={loading}
          >
            {loading ? t('schema.refreshing') : t('schema.actions.refresh')}
          </button>
        </div>
      </div>

      {error && (
        <div className="error-message">
          <span className="error-icon">❌</span>
          <span>{error}</span>
        </div>
      )}

      {schema ? (
        <div className="schema-content">
          <div className="schema-summary">
            <div className="summary-stats">
              <div className="stat-item">
                <span className="stat-value">{schema.schemas.length}</span>
                <span className="stat-label">{t('schema.schemasCount')}</span>
              </div>
              <div className="stat-item">
                <span className="stat-value">{schema.tables.length}</span>
                <span className="stat-label">{t('schema.tablesCount')}</span>
              </div>
              <div className="stat-item">
                <span className="stat-value">
                  {schema.tables.reduce((sum, table) => sum + table.columns.length, 0)}
                </span>
                <span className="stat-label">{t('schema.columnsCount')}</span>
              </div>
            </div>
            
            <div className="last-updated">
              <span className="update-label">{t('schema.lastUpdated')}</span>
              <span className="update-time">{formatDate(schema.lastUpdatedAt)}</span>
            </div>
          </div>

          <div className="schema-search">
            <input
              type="text"
              placeholder={t('schema.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
          </div>

          <div className="tables-view">
            {Object.entries(groupedTables).map(([schemaName, tables]) => (
              <div key={schemaName} className="schema-group">
                <h4 className="schema-group-title">
                  {schemaName} <span className="table-count">({tables.length})</span>
                </h4>
                
                <div className="tables-list">
                  {tables.map(table => {
                    const tableKey = getTableKey(table)
                    const isExpanded = expandedTables.has(tableKey)
                    
                    return (
                      <div key={tableKey} className="table-item">
                        <div 
                          className="table-header"
                          onClick={() => toggleTableExpansion(tableKey)}
                        >
                          <div className="table-info">
                            <span className="table-name">{table.tableName}</span>
                            <span className="table-type">{table.tableType}</span>
                            <span className="columns-count">
                              {t('schema.columnsInTable', { count: table.columns.length })}
                            </span>
                          </div>
                          <div className="expand-icon">
                            {isExpanded ? '▼' : '▶'}
                          </div>
                        </div>
                        
                        {table.comment && (
                          <div className="table-comment">
                            {table.comment}
                          </div>
                        )}
                        
                        {isExpanded && (
                          <div className="columns-list">
                            <div className="columns-header">
                              <span className="column-name-header">{t('schema.columnName')}</span>
                              <span className="column-type-header">{t('schema.dataType')}</span>
                              <span className="column-attrs-header">{t('schema.attributes')}</span>
                            </div>
                            {table.columns.map(column => (
                              <div key={column.columnName} className="column-item">
                                <div className="column-name">
                                  {column.columnName}
                                  {column.primaryKey && <span className="pk-badge">PK</span>}
                                </div>
                                <div className="column-type">
                                  {column.dataType}
                                  {column.columnSize && `(${column.columnSize})`}
                                </div>
                                <div className="column-attrs">
                                  {!column.nullable && <span className="not-null-badge">NOT NULL</span>}
                                  {column.autoIncrement && <span className="auto-inc-badge">AUTO</span>}
                                  {column.defaultValue && (
                                    <span className="default-badge">
                                      DEFAULT: {column.defaultValue}
                                    </span>
                                  )}
                                </div>
                                {column.comment && (
                                  <div className="column-comment">
                                    {column.comment}
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="no-schema-state">
          <div className="empty-icon">📊</div>
          <h3>{t('schema.noSchemaData')}</h3>
          <p>{t('schema.noSchemaDataDescription')}</p>
          <button
            className="button button-primary"
            onClick={onReadSchema}
            disabled={loading}
          >
            {loading ? t('schema.reading') : t('schema.actions.readFirst')}
          </button>
        </div>
      )}
    </div>
  )
}
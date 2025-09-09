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

import React, {useState} from 'react'
import {useTranslation} from 'react-i18next'
import {FaKey, FaLock} from 'react-icons/fa'
import type {SchemaMetadata, TableMetadata} from '../types/frontend'

interface SchemaMetadataViewProps {
  schema: SchemaMetadata | null
  loading: boolean
  error: string | null
  onRefreshSchema: () => void
}

export const SchemaMetadataView: React.FC<SchemaMetadataViewProps> = (
  {
    schema,
    loading,
    error,
    onRefreshSchema
  }
) => {
  const {t} = useTranslation()
  const [selectedTable, setSelectedTable] = useState<TableMetadata | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [activeDetailTab, setActiveDetailTab] = useState<'columns' | 'table'>('columns')

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  }

  const handleTableSelect = (table: TableMetadata) => {
    setSelectedTable(table)
    // テーブル選択時はカラム情報タブに戻す
    setActiveDetailTab('columns')
  }

  const renderTableMetadata = (table: TableMetadata) => {
    return (
      <div className="metadata-view">
        <div className="table-info-header">
          <h3>{table.tableName}</h3>
        </div>
        
        <div className="detail-tabs">
          <ul className="tabs-list">
            <li className={`tab-item ${activeDetailTab === 'columns' ? 'active' : ''}`}>
              <button
                type="button"
                className="tab-button"
                onClick={() => setActiveDetailTab('columns')}
              >
                {t('schema.columnInformation')}
              </button>
            </li>
            <li className={`tab-item ${activeDetailTab === 'table' ? 'active' : ''}`}>
              <button
                type="button"
                className="tab-button"
                onClick={() => setActiveDetailTab('table')}
              >
                {t('schema.tableInformation')}
              </button>
            </li>
          </ul>
        </div>
        
        <div className="detail-tab-content">
          {activeDetailTab === 'columns' && (
            <div className="column-metadata-section">
              <div className="table-container">
                <table className="table metadata-table table-striped">
                  <thead className="metadata-table-header">
                    <tr>
                      <th className="column-name-header">{t('metadata.columnName')}</th>
                      <th className="data-type-header">{t('metadata.dataType')}</th>
                      <th className="nullable-header">{t('metadata.nullable')}</th>
                      <th className="default-value-header">{t('metadata.defaultValue')}</th>
                      <th className="primary-key-header">{t('metadata.primaryKey')}</th>
                      <th className="comment-header">{t('metadata.comment')}</th>
                    </tr>
                  </thead>
                  <tbody className="metadata-table-body">
                    {table.columns.map((column, index) => (
                      <tr key={column.columnName} className={index % 2 === 0 ? 'even' : 'odd'}>
                        <td className="column-name-cell">
                          <div className="column-name-info">
                            <span className="column-name">{column.columnName}</span>
                            {column.primaryKey && (
                              <span className="pk-badge" title={t('metadata.primaryKey')}>
                                <FaKey/>
                              </span>
                            )}
                            {!column.nullable && (
                              <span className="not-null-badge" title={t('metadata.notNull')}>
                                <FaLock/>
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="data-type-cell">
                          <code className="data-type">
                            {column.dataType}
                            {column.columnSize && `(${column.columnSize})`}
                          </code>
                        </td>
                        <td className="nullable-cell">
                          <span className={`nullable-badge ${column.nullable ? 'nullable' : 'not-nullable'}`}>
                            {column.nullable ? t('common.yes') : t('common.no')}
                          </span>
                        </td>
                        <td className="default-value-cell">
                          {column.defaultValue ? (
                            <code className="default-value">{column.defaultValue}</code>
                          ) : (
                            <span className="no-default">—</span>
                          )}
                        </td>
                        <td className="primary-key-cell">
                          <span className={`pk-badge ${column.primaryKey ? 'is-pk' : 'not-pk'}`}>
                            {column.primaryKey ? (
                              <><FaKey/> {t('common.yes')}</>
                            ) : (
                              t('common.no')
                            )}
                          </span>
                        </td>
                        <td className="comment-cell">
                          {column.comment ? (
                            <span className="comment-text">{column.comment}</span>
                          ) : (
                            <span className="no-comment">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
          
          {activeDetailTab === 'table' && (
            <div className="table-metadata-section">
              <div className="table-container">
                <table className="table metadata-table table-striped">
                  <tbody className="metadata-table-body">
                    <tr className="even">
                      <td className="meta-label">{t('schema.schemaName')}</td>
                      <td className="meta-value">{table.schema}</td>
                    </tr>
                    <tr className="odd">
                      <td className="meta-label">{t('schema.tableName')}</td>
                      <td className="meta-value">{table.tableName}</td>
                    </tr>
                    <tr className="even">
                      <td className="meta-label">{t('schema.tableType')}</td>
                      <td className="meta-value">{table.tableType}</td>
                    </tr>
                    <tr className="odd">
                      <td className="meta-label">{t('schema.columnCount')}</td>
                      <td className="meta-value">{table.columns.length}</td>
                    </tr>
                    <tr className="even">
                      <td className="meta-label">{t('metadata.comment')}</td>
                      <td className="meta-value">
                        {table.comment ? table.comment : <span className="no-comment">—</span>}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    )
  }

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
        <div className="schema-actions">
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

          <div className="schema-layout">
            <div className="tables-sidebar">
              <div className="tables-view">
                {Object.entries(groupedTables).map(([schemaName, tables]) => (
                  <div key={schemaName} className="schema-group">
                    <h4 className="schema-group-title">
                      {schemaName} <span className="table-count">({tables.length})</span>
                    </h4>

                    <div className="tables-list">
                      {tables.map(table => (
                        <div
                          key={`${table.schema}.${table.tableName}`}
                          className={`table-item ${selectedTable?.tableName === table.tableName && selectedTable?.schema === table.schema ? 'selected' : ''}`}
                          onClick={() => handleTableSelect(table)}
                        >
                          <div className="table-header">
                            <div className="table-info">
                              <span className="table-name">{table.tableName}</span>
                              <div className="table-meta-row">
                                <span className="columns-count">
                                  {t('schema.columnsInTable', {count: table.columns.length})}
                                </span>
                                <span className="table-type">{table.tableType}</span>
                              </div>
                            </div>
                          </div>
                          {table.comment && (
                            <div className="table-comment">
                              {table.comment}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="table-detail-panel">
              {selectedTable ? (
                renderTableMetadata(selectedTable)
              ) : (
                <div className="no-table-selected">
                  <div className="empty-icon">📋</div>
                  <h3>{t('schema.selectTable')}</h3>
                  <p>{t('schema.selectTableDescription')}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      ) : (
        <div className="no-schema-state">
          <div className="empty-icon">📊</div>
          <h3>{t('schema.noSchemaData')}</h3>
          <p>{t('schema.noSchemaDataDescription')}</p>
        </div>
      )}
    </div>
  )
}

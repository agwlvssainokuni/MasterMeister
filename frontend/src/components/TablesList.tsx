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
import {dataAccessService} from '../services/dataAccessService'
import type {AccessibleTable} from '../types/frontend'
import '../styles/components/Table.css'

interface TablesListProps {
  connectionId: number
  onTableSelect: (table: AccessibleTable) => void
  selectedTable?: AccessibleTable
}

export const TablesList: React.FC<TablesListProps> = (
  {connectionId, onTableSelect, selectedTable}
) => {
  const {t} = useTranslation()
  const [tables, setTables] = useState<AccessibleTable[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filterText, setFilterText] = useState('')

  useEffect(() => {
    const loadTables = async () => {
      try {
        setLoading(true)
        setError(null)
        const tablesData = await dataAccessService.getAccessibleTables(connectionId)
        setTables(tablesData)
      } catch (err) {
        console.error('Error loading tables:', err)
        setError(err instanceof Error ? err.message : 'Failed to load tables')
      } finally {
        setLoading(false)
      }
    }

    if (connectionId) {
      loadTables()
    }
  }, [connectionId])

  const filteredTables = tables.filter(table =>
    table.fullTableName.toLowerCase().includes(filterText.toLowerCase()) ||
    table.schemaName.toLowerCase().includes(filterText.toLowerCase()) ||
    table.tableName.toLowerCase().includes(filterText.toLowerCase())
  )

  const getPermissionIcons = (table: AccessibleTable) => {
    const icons = []
    if (table.canRead) icons.push('👁️')
    if (table.canWrite) icons.push('✏️')
    if (table.canDelete) icons.push('🗑️')
    if (table.canAdmin) icons.push('⚙️')
    return icons.join(' ')
  }

  const getTableTypeIcon = (tableType: string) => {
    switch (tableType.toLowerCase()) {
      case 'table':
        return '🏷️'
      case 'view':
        return '👁️'
      case 'system table':
        return '⚙️'
      default:
        return '📄'
    }
  }

  if (loading) {
    return (
      <div className="tables-list-container">
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p>{t('common.loading')}</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="tables-list-container">
        <div className="error-state">
          <p className="error-message">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="tables-list-container">
      <div className="tables-list-header">
        <h3>{t('tablesList.title')}</h3>
        <div className="search-box">
          <input
            type="text"
            placeholder={t('tablesList.searchPlaceholder')}
            value={filterText}
            onChange={(e) => setFilterText(e.target.value)}
            className="search-input"
          />
        </div>
        <div className="tables-count">
          {t('tablesList.tableCount', {count: filteredTables.length, total: tables.length})}
        </div>
      </div>

      <div className="tables-list">
        {filteredTables.length === 0 ? (
          <div className="empty-state">
            <p>{filterText ? t('tablesList.noTablesFound') : t('tablesList.noTables')}</p>
          </div>
        ) : (
          filteredTables.map(table => (
            <div
              key={`${table.schemaName}.${table.tableName}`}
              className={`table-item ${selectedTable?.tableName === table.tableName && selectedTable?.schemaName === table.schemaName ? 'selected' : ''}`}
              onClick={() => onTableSelect(table)}
            >
              <div className="table-item-header">
                <span className="table-type-icon">{getTableTypeIcon(table.tableType)}</span>
                <span className="table-name">{table.fullTableName}</span>
                <span className="permissions">{getPermissionIcons(table)}</span>
              </div>

              <div className="table-item-details">
                <div className="table-info">
                  <span className="schema-name">{table.schemaName}</span>
                  <span className="table-type">{table.tableType}</span>
                </div>

                {table.comment && (
                  <div className="table-comment">
                    {table.comment}
                  </div>
                )}

                <div className="permission-details">
                  {table.canRead && <span className="permission-badge read">READ</span>}
                  {table.canWrite && <span className="permission-badge write">WRITE</span>}
                  {table.canDelete && <span className="permission-badge delete">DELETE</span>}
                  {table.canAdmin && <span className="permission-badge admin">ADMIN</span>}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

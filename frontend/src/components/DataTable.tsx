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

import React, {useCallback, useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {dataAccessService} from '../services/dataAccessService'
import {ColumnFilterComponent} from './ColumnFilter'
import {PermissionGuard} from './PermissionGuard'
import type {
  AccessibleTable,
  ColumnFilter,
  ColumnMetadata,
  RecordFilter,
  RecordQueryData,
  SortOrder,
  TableRecord
} from '../types/frontend'
import '../styles/components/Table.css'

interface DataTableProps {
  connectionId: number
  schemaName: string
  tableName: string
  accessibleTable: AccessibleTable
  onRecordSelect?: (record: TableRecord) => void
  onRecordEdit?: (record: TableRecord) => void
  onRecordDelete?: (record: TableRecord) => void
  onRecordCreate?: () => void
  onDataReload?: () => void
}

export const DataTable: React.FC<DataTableProps> = (
  {
    connectionId,
    schemaName,
    tableName,
    accessibleTable,
    onRecordSelect,
    onRecordEdit,
    onRecordDelete,
    onRecordCreate,
    onDataReload
  }
) => {
  const {t} = useTranslation()
  const [queryData, setQueryData] = useState<RecordQueryData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [sortOrders, setSortOrders] = useState<SortOrder[]>([])
  const [columnFilters, setColumnFilters] = useState<ColumnFilter[]>([])
  const [selectedRecords, setSelectedRecords] = useState<Set<number>>(new Set())

  const loadRecords = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)

      const filter: RecordFilter = {
        columnFilters,
        sortOrders,
        customWhere: undefined
      }

      const data = await dataAccessService.getRecords(
        connectionId,
        schemaName,
        tableName,
        currentPage,
        pageSize,
        filter
      )

      setQueryData(data)
    } catch (err) {
      console.error('Error loading records:', err)
      setError(err instanceof Error ? err.message : 'Failed to load records')
    } finally {
      setLoading(false)
    }
  }, [connectionId, schemaName, tableName, currentPage, pageSize, columnFilters, sortOrders])

  useEffect(() => {
    loadRecords()
  }, [loadRecords])

  // Expose reload function to parent
  useEffect(() => {
    if (onDataReload) {
      onDataReload()
    }
  }, [onDataReload])

  const handleSort = (columnName: string) => {
    setSortOrders(prev => {
      const existing = prev.find(so => so.columnName === columnName)
      if (existing) {
        if (existing.direction === 'ASC') {
          return prev.map(so =>
            so.columnName === columnName
              ? {...so, direction: 'DESC' as const}
              : so
          )
        } else {
          return prev.filter(so => so.columnName !== columnName)
        }
      } else {
        return [...prev, {columnName, direction: 'ASC' as const}]
      }
    })
  }

  const handlePageChange = (newPage: number) => {
    setCurrentPage(newPage)
    setSelectedRecords(new Set())
  }

  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize)
    setCurrentPage(0)
    setSelectedRecords(new Set())
  }

  const handleFilterChange = (columnName: string, filter: ColumnFilter | null) => {
    setColumnFilters(prev => {
      if (filter === null) {
        return prev.filter(f => f.columnName !== columnName)
      } else {
        const existing = prev.find(f => f.columnName === columnName)
        if (existing) {
          return prev.map(f => f.columnName === columnName ? filter : f)
        } else {
          return [...prev, filter]
        }
      }
    })
    setCurrentPage(0) // Reset to first page when filter changes
    setSelectedRecords(new Set())
  }

  const getCurrentFilter = (columnName: string): ColumnFilter | undefined => {
    return columnFilters.find(f => f.columnName === columnName)
  }

  const handleRecordSelect = (index: number, record: TableRecord) => {
    const newSelected = new Set(selectedRecords)
    if (newSelected.has(index)) {
      newSelected.delete(index)
    } else {
      newSelected.add(index)
    }
    setSelectedRecords(newSelected)

    if (onRecordSelect && newSelected.has(index)) {
      onRecordSelect(record)
    }
  }

  const handleSelectAll = () => {
    if (!queryData) return

    if (selectedRecords.size === queryData.records.length) {
      setSelectedRecords(new Set())
    } else {
      setSelectedRecords(new Set(Array.from({length: queryData.records.length}, (_, i) => i)))
    }
  }

  const getSortIcon = (columnName: string) => {
    const sortOrder = sortOrders.find(so => so.columnName === columnName)
    if (!sortOrder) return '↕️'
    return sortOrder.direction === 'ASC' ? '↑' : '↓'
  }

  const formatCellValue = (value: unknown, column: ColumnMetadata): string => {
    if (value === null || value === undefined) {
      return ''
    }

    if (typeof value === 'boolean') {
      return value ? 'true' : 'false'
    }

    if (column.dataType.toLowerCase().includes('date') || column.dataType.toLowerCase().includes('time')) {
      try {
        return new Date(value.toString()).toLocaleString()
      } catch {
        return String(value)
      }
    }

    return String(value)
  }

  if (loading) {
    return (
      <div className="data-table-container">
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p>{t('common.loading')}</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="data-table-container">
        <div className="error-state">
          <p className="error-message">{error}</p>
          <button onClick={loadRecords} className="button button-primary">
            {t('common.retry')}
          </button>
        </div>
      </div>
    )
  }

  if (!queryData) {
    return (
      <div className="data-table-container">
        <div className="empty-state">
          <p>{t('dataTable.noData')}</p>
        </div>
      </div>
    )
  }

  const {records, accessibleColumns, totalRecords, totalPages, hasNextPage, hasPreviousPage} = queryData

  return (
    <div className="data-table-container">
      <div className="table-header">
        <div className="table-info">
          <h2>{accessibleTable.fullTableName}</h2>
          <div className="table-stats">
            <span className="record-count">
              {t('dataTable.recordCount', {count: totalRecords})}
            </span>
            <span className="execution-time">
              {t('dataTable.executionTime', {time: queryData.executionTimeMs})}
            </span>
          </div>
        </div>

        <div className="table-actions">
          <PermissionGuard table={accessibleTable} requiredPermission="write">
            <button className="button button-primary" onClick={() => onRecordCreate?.()}>
              {t('dataTable.createRecord')}
            </button>
          </PermissionGuard>
        </div>
      </div>

      <div className="table-controls">
        <div className="page-size-control">
          <label>
            {t('dataTable.pageSize')}:
            <select value={pageSize} onChange={(e) => handlePageSizeChange(Number(e.target.value))}>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
        </div>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
          <tr>
            <th className="select-column">
              <input
                type="checkbox"
                checked={selectedRecords.size === records.length && records.length > 0}
                onChange={handleSelectAll}
              />
            </th>
            {accessibleColumns.map(column => (
              <th key={column.columnName} className="sortable">
                <div className="column-header">
                  <div className="column-name-section" onClick={() => handleSort(column.columnName)}>
                    <span className="column-name">{column.columnName}</span>
                    <span className="sort-icon">{getSortIcon(column.columnName)}</span>
                    {column.primaryKey && <span className="pk-icon">🔑</span>}
                  </div>
                  <div className="column-filter-section">
                    <ColumnFilterComponent
                      column={column}
                      currentFilter={getCurrentFilter(column.columnName)}
                      onFilterChange={(filter) => handleFilterChange(column.columnName, filter)}
                    />
                  </div>
                </div>
                <div className="column-info">
                  <span className="data-type">{column.dataType}</span>
                  {!column.nullable && <span className="not-null">NOT NULL</span>}
                </div>
              </th>
            ))}
            <ConditionalPermission table={accessibleTable} requiredPermission="write">
              {(hasWritePermission) => (
                hasWritePermission || accessibleTable.canDelete ? (
                  <th className="actions-column">{t('common.actions')}</th>
                ) : null
              )}
            </ConditionalPermission>
          </tr>
          </thead>
          <tbody>
          {records.map((record, index) => (
            <tr
              key={index}
              className={selectedRecords.has(index) ? 'selected' : ''}
            >
              <td className="select-column">
                <input
                  type="checkbox"
                  checked={selectedRecords.has(index)}
                  onChange={() => handleRecordSelect(index, record)}
                />
              </td>
              {accessibleColumns.map(column => (
                <td key={column.columnName} className="data-cell">
                  {formatCellValue(record[column.columnName], column)}
                </td>
              ))}
              <ConditionalPermission table={accessibleTable} requiredPermission="write">
                {(hasWritePermission) => (
                  hasWritePermission || accessibleTable.canDelete ? (
                    <td className="actions-column">
                      <div className="action-buttons">
                        <PermissionGuard table={accessibleTable} requiredPermission="write">
                          <button
                            className="button button-sm button-secondary"
                            onClick={() => onRecordEdit?.(record)}
                            title={t('common.edit')}
                          >
                            ✏️
                          </button>
                        </PermissionGuard>
                        <PermissionGuard table={accessibleTable} requiredPermission="delete">
                          <button
                            className="button button-sm button-danger"
                            onClick={() => onRecordDelete?.(record)}
                            title={t('common.delete')}
                          >
                            🗑️
                          </button>
                        </PermissionGuard>
                      </div>
                    </td>
                  ) : null
                )}
              </ConditionalPermission>
            </tr>
          ))}
          </tbody>
        </table>
      </div>

      <div className="table-pagination">
        <div className="pagination-info">
          {t('dataTable.pageInfo', {
            current: currentPage + 1,
            total: totalPages,
            totalRecords
          })}
        </div>
        <div className="pagination-controls">
          <button
            className="button button-sm"
            disabled={!hasPreviousPage}
            onClick={() => handlePageChange(currentPage - 1)}
          >
            {t('common.previous')}
          </button>
          <span className="page-indicator">
            {currentPage + 1} / {totalPages}
          </span>
          <button
            className="button button-sm"
            disabled={!hasNextPage}
            onClick={() => handlePageChange(currentPage + 1)}
          >
            {t('common.next')}
          </button>
        </div>
      </div>
    </div>
  )
}

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

import React, {useCallback, useEffect, useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {FaEdit, FaKey, FaSort, FaSortDown, FaSortUp, FaTrash} from 'react-icons/fa'
import {dataAccessService} from '../services/dataAccessService'
import {ConditionalPermission} from './PermissionGuard'
import type {TabItem} from './Tabs'
import {Tabs} from './Tabs'
import {TableMetadataView} from './TableMetadataView'
import {TableFilterBarView} from './TableFilterBarView'
import type {
  AccessibleTable,
  ColumnFilter,
  ColumnMetadata,
  RecordFilter,
  RecordQueryData,
  SortOrder,
  TableRecord
} from '../types/frontend'

interface DataTableViewProps {
  connectionId: number
  schemaName: string
  tableName: string
  accessibleTable: AccessibleTable
  reloadTrigger?: number
  onRecordEdit?: (record: TableRecord) => void
  onRecordDelete?: (record: TableRecord) => void
  onRecordCreate?: () => void
}

export const DataTableView: React.FC<DataTableViewProps> = (
  {
    connectionId,
    schemaName,
    tableName,
    accessibleTable,
    reloadTrigger,
    onRecordEdit,
    onRecordDelete,
    onRecordCreate,
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
  const [pendingFilters, setPendingFilters] = useState<ColumnFilter[]>([])
  const [activeTab, setActiveTab] = useState<string>('data')

  // Use refs to avoid infinite loops in useCallback dependencies
  const columnFiltersRef = useRef<ColumnFilter[]>([])
  const sortOrdersRef = useRef<SortOrder[]>([])

  // Update refs when state changes
  useEffect(() => {
    columnFiltersRef.current = columnFilters
  }, [columnFilters])

  useEffect(() => {
    sortOrdersRef.current = sortOrders
  }, [sortOrders])

  const loadRecords = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)

      const filter: RecordFilter = {
        columnFilters: columnFiltersRef.current,
        sortOrders: sortOrdersRef.current,
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
  }, [connectionId, schemaName, tableName, currentPage, pageSize])

  useEffect(() => {
    loadRecords()
  }, [loadRecords])

  // Reload when parent triggers reload via reloadTrigger prop
  useEffect(() => {
    if (reloadTrigger && reloadTrigger > 0) {
      loadRecords()
    }
  }, [reloadTrigger, loadRecords])

  // Trigger reload when filters or sort orders change
  const [filterChangeCount, setFilterChangeCount] = useState(0)
  const prevColumnFiltersRef = useRef<ColumnFilter[]>([])
  const prevSortOrdersRef = useRef<SortOrder[]>([])

  useEffect(() => {
    // Check if filters or sorts have actually changed
    const filtersChanged = JSON.stringify(columnFilters) !== JSON.stringify(prevColumnFiltersRef.current)
    const sortsChanged = JSON.stringify(sortOrders) !== JSON.stringify(prevSortOrdersRef.current)

    if (filtersChanged || sortsChanged) {
      prevColumnFiltersRef.current = columnFilters
      prevSortOrdersRef.current = sortOrders
      setFilterChangeCount(prev => prev + 1)
    }
  }, [columnFilters, sortOrders])

  useEffect(() => {
    // Reload when filter change counter increases
    if (filterChangeCount > 0) {
      loadRecords()
    }
  }, [filterChangeCount, loadRecords])

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
  }

  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize)
    setCurrentPage(0)
  }

  const handlePendingFilterChange = (columnName: string, filter: ColumnFilter | null) => {
    setPendingFilters(prev => {
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
  }

  const handleFilterClear = (columnName: string) => {
    setColumnFilters(prev => prev.filter(f => f.columnName !== columnName))
    setPendingFilters(prev => prev.filter(f => f.columnName !== columnName))
    setCurrentPage(0)
  }

  const handleApplyAllFilters = () => {
    setColumnFilters(pendingFilters)
    setCurrentPage(0)
  }

  const handleClearAllFilters = () => {
    setColumnFilters([])
    setPendingFilters([])
    setCurrentPage(0)
  }


  const getSortIcon = (columnName: string) => {
    const sortOrder = sortOrders.find(so => so.columnName === columnName)
    if (!sortOrder) return <FaSort/>
    return sortOrder.direction === 'ASC' ? <FaSortUp/> : <FaSortDown/>
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

  // Create tab items
  const tabItems: TabItem[] = [
    {
      id: 'data',
      label: t('tabs.data'),
      content: (
        <div className="data-tab-content">
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
            <table className="data-table table-striped">
              <thead className="data-table-header">
              <tr>
                <th className="actions-column">{t('common.actions')}</th>
                {accessibleColumns.map(column => {
                  const tooltipText = [
                    column.dataType,
                    !column.nullable ? 'NOT NULL' : null,
                    column.primaryKey ? 'PRIMARY KEY' : null
                  ].filter(Boolean).join(', ')

                  const columnClass = column.primaryKey ? 'sortable pk-column' : 'sortable'

                  return (
                    <th key={column.columnName} className={columnClass} onClick={() => handleSort(column.columnName)}>
                      <div className="simple-column-header">
                        <span className="column-name">{column.columnName}</span>
                        {column.primaryKey && <span className="pk-icon"><FaKey/></span>}
                        <span className="sort-icon">{getSortIcon(column.columnName)}</span>
                      </div>
                      <div className="column-type-info" data-tooltip={tooltipText}>
                        <div className="data-type">{column.dataType}</div>
                        <div className="not-null">
                          {!column.nullable ? 'NOT NULL' : ''}
                        </div>
                      </div>
                    </th>
                  )
                })}
              </tr>
              </thead>
              <tbody className="data-table-body">
              {records.map((record, index) => (
                <tr key={index}>
                  <td className="actions-column">
                    <div className="action-buttons">
                      <ConditionalPermission table={accessibleTable} requiredPermission="write">
                        {(hasWritePermission) => (
                          <button
                            className="button button-sm button-secondary"
                            disabled={!hasWritePermission}
                            onClick={() => hasWritePermission && onRecordEdit?.(record)}
                            title={hasWritePermission ? t('common.edit') : t('permissions.insufficientPermissions')}
                          >
                            <FaEdit/>
                          </button>
                        )}
                      </ConditionalPermission>
                      <ConditionalPermission table={accessibleTable} requiredPermission="delete">
                        {(hasDeletePermission) => (
                          <button
                            className="button button-sm button-danger"
                            disabled={!hasDeletePermission}
                            onClick={() => hasDeletePermission && onRecordDelete?.(record)}
                            title={hasDeletePermission ? t('common.delete') : t('permissions.insufficientPermissions')}
                          >
                            <FaTrash/>
                          </button>
                        )}
                      </ConditionalPermission>
                    </div>
                  </td>
                  {accessibleColumns.map(column => (
                    <td key={column.columnName} className={column.primaryKey ? 'data-cell pk-column' : 'data-cell'}>
                      {formatCellValue(record[column.columnName], column)}
                    </td>
                  ))}
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

          <TableFilterBarView
            columns={accessibleColumns}
            activeFilters={columnFilters}
            pendingFilters={pendingFilters}
            onFilterChange={handlePendingFilterChange}
            onFilterClear={handleFilterClear}
            onClearAllFilters={handleClearAllFilters}
            onApplyAllFilters={handleApplyAllFilters}
          />
        </div>
      )
    },
    {
      id: 'metadata',
      label: t('tabs.metadata'),
      content: (
        <TableMetadataView
          columns={accessibleColumns}
        />
      )
    }
  ]

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
          <ConditionalPermission table={accessibleTable} requiredPermission="write">
            {(hasWritePermission) => (
              <button
                className="button button-primary"
                disabled={!hasWritePermission}
                onClick={() => hasWritePermission && onRecordCreate?.()}
                title={hasWritePermission ? t('dataTable.createRecord') : t('permissions.insufficientPermissions')}
              >
                {t('dataTable.createRecord')}
              </button>
            )}
          </ConditionalPermission>
        </div>
      </div>

      <Tabs
        items={tabItems}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        className="table-tabs"
      />
    </div>
  )
}

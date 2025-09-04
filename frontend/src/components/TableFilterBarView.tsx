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
import {FaTimes} from 'react-icons/fa'
import {ColumnFilterComponent} from './ColumnFilter'
import type {ColumnFilter, ColumnMetadata} from '../types/frontend'

interface TableFilterBarProps {
  columns: ColumnMetadata[]
  activeFilters: ColumnFilter[]
  pendingFilters?: ColumnFilter[]
  onFilterChange: (columnName: string, filter: ColumnFilter | null) => void
  onFilterClear?: (columnName: string) => void
  onClearAllFilters: () => void
  onApplyAllFilters: () => void
}

export const TableFilterBarView: React.FC<TableFilterBarProps> = (
  {
    columns,
    activeFilters,
    pendingFilters = [],
    onFilterChange,
    onFilterClear,
    onClearAllFilters,
    onApplyAllFilters,
  }
) => {
  const {t} = useTranslation()

  const getCurrentFilter = (columnName: string): ColumnFilter | undefined => {
    return activeFilters.find(f => f.columnName === columnName)
  }

  const getPendingFilter = (columnName: string): ColumnFilter | undefined => {
    return pendingFilters.find(f => f.columnName === columnName)
  }

  const hasActiveFilters = activeFilters.length > 0

  const operatorToSqlExpression = (operator: string): string => {
    switch (operator) {
      case 'EQUALS': return '='
      case 'NOT_EQUALS': return '!='
      case 'GREATER_THAN': return '>'
      case 'LESS_THAN': return '<'
      case 'GREATER_EQUALS': return '>='
      case 'LESS_EQUALS': return '<='
      case 'LIKE': return 'LIKE'
      case 'NOT_LIKE': return 'NOT LIKE'
      case 'BETWEEN': return 'BETWEEN'
      case 'IN': return 'IN'
      case 'NOT_IN': return 'NOT IN'
      case 'IS_NULL': return 'IS NULL'
      case 'IS_NOT_NULL': return 'IS NOT NULL'
      default: return operator
    }
  }

  return (
    <div className="table-filter-bar">
      <div className="filter-bar-header">
        <h4 className="filter-bar-title">{t('dataTable.filters.title')}</h4>
        <div className="filter-bar-actions">
          <button
            className="button button-sm button-primary"
            onClick={onApplyAllFilters}
            title={t('dataTable.filters.applyAll')}
          >
            {t('dataTable.filters.applyAll')}
          </button>
          {hasActiveFilters && (
            <button
              className="button button-sm button-secondary"
              onClick={onClearAllFilters}
              title={t('dataTable.filters.clearAll')}
            >
              <FaTimes/> {t('dataTable.filters.clearAll')}
            </button>
          )}
        </div>
      </div>

      {hasActiveFilters && (
        <div className="active-filters">
          <span className="active-filters-label">{t('dataTable.filters.active')}:</span>
          <div className="filter-chips">
            {activeFilters.map(filter => (
                <div key={filter.columnName} className="filter-chip">
                  <span className="filter-chip-column">{filter.columnName}</span>
                  <span className="filter-chip-operator">{operatorToSqlExpression(filter.operator)}</span>
                  <span className="filter-chip-value">
                    {filter.value !== undefined ? String(filter.value) : ''}
                    {filter.operator === 'BETWEEN' && filter.value2 !== undefined && (
                      <> AND {String(filter.value2)}</>
                    )}
                  </span>
                  <button
                    className="filter-chip-remove"
                    onClick={() => onFilterChange(filter.columnName, null)}
                    title={t('dataTable.filters.remove')}
                  >
                    <FaTimes/>
                  </button>
                </div>
              ))}
          </div>
        </div>
      )}

      <div className="filter-controls">
        <div className="filter-controls-grid">
          {columns.map(column => (
            <div key={column.columnName} className="filter-control-item">
              <div className="filter-control-header">
                <span className="column-name">{column.columnName}</span>
                <span className="column-info">
                  {column.dataType}
                  {column.primaryKey && <span className="pk-badge">PK</span>}
                  {!column.nullable && <span className="not-null-badge">NOT NULL</span>}
                </span>
              </div>
              <div className="filter-control-input">
                <ColumnFilterComponent
                  column={column}
                  currentFilter={getCurrentFilter(column.columnName)}
                  pendingFilter={getPendingFilter(column.columnName)}
                  onPendingFilterChange={(filter) => onFilterChange(column.columnName, filter)}
                  onFilterClear={() => onFilterClear?.(column.columnName)}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

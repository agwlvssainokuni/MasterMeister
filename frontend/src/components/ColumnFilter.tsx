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
import type {AccessibleColumn, ColumnFilter} from '../types/frontend'
import '../styles/components/ColumnFilter.css'

interface ColumnFilterProps {
  column: AccessibleColumn
  currentFilter?: ColumnFilter
  pendingFilter?: ColumnFilter
  onPendingFilterChange: (filter: ColumnFilter | null) => void
  onFilterClear: () => void
}

export const ColumnFilterComponent: React.FC<ColumnFilterProps> = (
  {
    column,
    currentFilter,
    pendingFilter,
    onPendingFilterChange,
    onFilterClear,
  }
) => {
  const {t} = useTranslation()
  const filterToUse = pendingFilter || currentFilter
  const [operator, setOperator] = useState<ColumnFilter['operator']>(filterToUse?.operator || 'EQUALS')
  const [value, setValue] = useState<string>(filterToUse?.value?.toString() || '')
  const [value2, setValue2] = useState<string>(filterToUse?.value2?.toString() || '')

  const getOperatorOptions = () => {
    const dataType = column.dataType.toLowerCase()

    if (dataType.includes('bool')) {
      return [
        {value: 'EQUALS', label: t('filter.operators.equals')},
        {value: 'IS_NULL', label: t('filter.operators.isNull')},
        {value: 'IS_NOT_NULL', label: t('filter.operators.isNotNull')}
      ]
    }

    if (dataType.includes('date') || dataType.includes('time') || dataType.includes('int') || dataType.includes('decimal')) {
      return [
        {value: 'EQUALS', label: t('filter.operators.equals')},
        {value: 'NOT_EQUALS', label: t('filter.operators.notEquals')},
        {value: 'GREATER_THAN', label: t('filter.operators.greaterThan')},
        {value: 'LESS_THAN', label: t('filter.operators.lessThan')},
        {value: 'GREATER_EQUALS', label: t('filter.operators.greaterEquals')},
        {value: 'LESS_EQUALS', label: t('filter.operators.lessEquals')},
        {value: 'BETWEEN', label: t('filter.operators.between')},
        {value: 'IS_NULL', label: t('filter.operators.isNull')},
        {value: 'IS_NOT_NULL', label: t('filter.operators.isNotNull')}
      ]
    }

    // String types
    return [
      {value: 'EQUALS', label: t('filter.operators.equals')},
      {value: 'NOT_EQUALS', label: t('filter.operators.notEquals')},
      {value: 'LIKE', label: t('filter.operators.like')},
      {value: 'NOT_LIKE', label: t('filter.operators.notLike')},
      {value: 'IN', label: t('filter.operators.in')},
      {value: 'NOT_IN', label: t('filter.operators.notIn')},
      {value: 'IS_NULL', label: t('filter.operators.isNull')},
      {value: 'IS_NOT_NULL', label: t('filter.operators.isNotNull')}
    ]
  }

  const getInputType = () => {
    const dataType = column.dataType.toLowerCase()

    if (dataType.includes('int') || dataType.includes('number') || dataType.includes('decimal')) {
      return 'number'
    }

    if (dataType.includes('date')) {
      return 'date'
    }

    if (dataType.includes('time') && !dataType.includes('timestamp')) {
      return 'time'
    }

    if (dataType.includes('timestamp') || dataType.includes('datetime')) {
      return 'datetime-local'
    }

    return 'text'
  }

  const needsValue = () => {
    return !['IS_NULL', 'IS_NOT_NULL'].includes(operator)
  }

  const needsSecondValue = () => {
    return operator === 'BETWEEN'
  }


  const convertValue = (val: string): string | number | boolean | string[] => {
    const dataType = column.dataType.toLowerCase()

    if (dataType.includes('int')) {
      return parseInt(val, 10)
    }

    if (dataType.includes('decimal') || dataType.includes('float') || dataType.includes('double')) {
      return parseFloat(val)
    }

    if (dataType.includes('bool')) {
      return val.toLowerCase() === 'true'
    }

    if (operator === 'IN' || operator === 'NOT_IN') {
      return val.split(',').map(v => v.trim())
    }

    return val
  }

  const handleClearFilter = () => {
    onFilterClear()
    setValue('')
    setValue2('')
    setOperator('EQUALS')
  }

  const handleOperatorChange = (newOperator: ColumnFilter['operator']) => {
    setOperator(newOperator)
    updatePendingFilter(newOperator, value, value2)
  }

  const handleValueChange = (newValue: string) => {
    setValue(newValue)
    updatePendingFilter(operator, newValue, value2)
  }

  const handleValue2Change = (newValue2: string) => {
    setValue2(newValue2)
    updatePendingFilter(operator, value, newValue2)
  }

  const updatePendingFilter = (op: ColumnFilter['operator'], val: string, val2: string) => {
    if (!needsValueForOperator(op)) {
      onPendingFilterChange({
        columnName: column.columnName,
        operator: op,
        value: undefined,
        value2: undefined
      })
    } else if (val.trim() === '') {
      onPendingFilterChange(null)
    } else {
      const convertedValue = convertValue(val)
      const convertedValue2 = needsSecondValueForOperator(op) && val2.trim() ? convertValue(val2) : undefined

      onPendingFilterChange({
        columnName: column.columnName,
        operator: op,
        value: convertedValue,
        value2: convertedValue2
      })
    }
  }

  const needsValueForOperator = (op: ColumnFilter['operator']) => {
    return !['IS_NULL', 'IS_NOT_NULL'].includes(op)
  }

  const needsSecondValueForOperator = (op: ColumnFilter['operator']) => {
    return op === 'BETWEEN'
  }

  const isFilterActive = currentFilter !== undefined && currentFilter !== null

  return (
    <div className="column-filter-inline">
      <div className="filter-operator">
        <select
          value={operator}
          onChange={(e) => handleOperatorChange(e.target.value as ColumnFilter['operator'])}
          className="operator-select"
        >
          {getOperatorOptions().map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {needsValue() && (
        <div className="filter-value">
          <input
            type={getInputType()}
            value={value}
            onChange={(e) => handleValueChange(e.target.value)}
            placeholder={
              operator === 'LIKE' || operator === 'NOT_LIKE'
                ? t('filter.likePlaceholder')
                : operator === 'IN' || operator === 'NOT_IN'
                  ? t('filter.inPlaceholder')
                  : t('filter.valuePlaceholder')
            }
            className="value-input"
          />
        </div>
      )}

      {needsSecondValue() && (
        <>
          <span className="filter-and-label">AND</span>
          <div className="filter-value2">
            <input
              type={getInputType()}
              value={value2}
              onChange={(e) => handleValue2Change(e.target.value)}
              placeholder={t('filter.valuePlaceholder')}
              className="value-input"
            />
          </div>
        </>
      )}

      {isFilterActive && (
        <button
          className="filter-clear-btn"
          onClick={handleClearFilter}
          title={t('filter.clear')}
        >
          ×
        </button>
      )}
    </div>
  )
}

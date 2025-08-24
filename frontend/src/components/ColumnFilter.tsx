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

import React, {useEffect, useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import type {ColumnFilter, ColumnMetadata} from '../types/frontend'
import '../styles/components/Table.css'

interface ColumnFilterProps {
  column: ColumnMetadata
  currentFilter?: ColumnFilter
  onFilterChange: (filter: ColumnFilter | null) => void
}

export const ColumnFilterComponent: React.FC<ColumnFilterProps> = ({
                                                                     column,
                                                                     currentFilter,
                                                                     onFilterChange
                                                                   }) => {
  const {t} = useTranslation()
  const [isOpen, setIsOpen] = useState(false)
  const [operator, setOperator] = useState<ColumnFilter['operator']>(currentFilter?.operator || 'EQUALS')
  const [value, setValue] = useState<string>(currentFilter?.value?.toString() || '')
  const [value2, setValue2] = useState<string>(currentFilter?.value2?.toString() || '')
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

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

  const handleApplyFilter = () => {
    if (!needsValue()) {
      onFilterChange({
        columnName: column.columnName,
        operator,
        value: undefined,
        value2: undefined
      })
    } else if (value.trim() === '') {
      onFilterChange(null)
    } else {
      const convertedValue = convertValue(value)
      const convertedValue2 = needsSecondValue() && value2.trim() ? convertValue(value2) : undefined

      onFilterChange({
        columnName: column.columnName,
        operator,
        value: convertedValue,
        value2: convertedValue2
      })
    }

    setIsOpen(false)
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
    onFilterChange(null)
    setValue('')
    setValue2('')
    setOperator('EQUALS')
    setIsOpen(false)
  }

  const isFilterActive = currentFilter !== undefined && currentFilter !== null

  return (
    <div className="column-filter" ref={dropdownRef}>
      <button
        className={`filter-button ${isFilterActive ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        title={t('filter.filterColumn')}
      >
        🔍
      </button>

      {isOpen && (
        <div className="filter-dropdown">
          <div className="filter-content">
            <div className="filter-header">
              <span className="filter-title">{t('filter.filterBy', {column: column.columnName})}</span>
              <button
                className="filter-close"
                onClick={() => setIsOpen(false)}
              >
                ×
              </button>
            </div>

            <div className="filter-body">
              <div className="filter-field">
                <label htmlFor={`operator-${column.columnName}`}>
                  {t('filter.operator')}:
                </label>
                <select
                  id={`operator-${column.columnName}`}
                  value={operator}
                  onChange={(e) => setOperator(e.target.value as ColumnFilter['operator'])}
                >
                  {getOperatorOptions().map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              {needsValue() && (
                <div className="filter-field">
                  <label htmlFor={`value-${column.columnName}`}>
                    {t('filter.value')}:
                  </label>
                  <input
                    id={`value-${column.columnName}`}
                    type={getInputType()}
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    placeholder={
                      operator === 'LIKE' || operator === 'NOT_LIKE'
                        ? t('filter.likePlaceholder')
                        : operator === 'IN' || operator === 'NOT_IN'
                          ? t('filter.inPlaceholder')
                          : t('filter.valuePlaceholder')
                    }
                  />
                </div>
              )}

              {needsSecondValue() && (
                <div className="filter-field">
                  <label htmlFor={`value2-${column.columnName}`}>
                    {t('filter.value2')}:
                  </label>
                  <input
                    id={`value2-${column.columnName}`}
                    type={getInputType()}
                    value={value2}
                    onChange={(e) => setValue2(e.target.value)}
                    placeholder={t('filter.valuePlaceholder')}
                  />
                </div>
              )}
            </div>

            <div className="filter-footer">
              <button
                className="button button-sm button-secondary"
                onClick={handleClearFilter}
              >
                {t('filter.clear')}
              </button>
              <button
                className="button button-sm button-primary"
                onClick={handleApplyFilter}
              >
                {t('filter.apply')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

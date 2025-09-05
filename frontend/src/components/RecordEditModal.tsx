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
import type {AccessibleColumn, RecordCreateData, RecordUpdateData, TableRecord} from '../types/frontend'

interface RecordEditModalProps {
  isOpen: boolean
  mode: 'create' | 'edit'
  connectionId: number
  schemaName: string
  tableName: string
  columns: AccessibleColumn[]
  existingRecord?: TableRecord
  onClose: () => void
  onSuccess: (message: string) => void
  onError: (error: string) => void
}

export const RecordEditModal: React.FC<RecordEditModalProps> = (
  {
    isOpen,
    mode,
    connectionId,
    schemaName,
    tableName,
    columns,
    existingRecord,
    onClose,
    onSuccess,
    onError,
  }
) => {
  const {t} = useTranslation()
  const [formData, setFormData] = useState<Record<string, unknown>>({})
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (isOpen) {
      if (mode === 'edit' && existingRecord) {
        setFormData({...existingRecord})
      } else {
        const initialData: Record<string, unknown> = {}
        columns.forEach(column => {
          if (column.defaultValue) {
            initialData[column.columnName] = column.defaultValue
          } else if (!column.nullable) {
            initialData[column.columnName] = ''
          }
        })
        setFormData(initialData)
      }
      setErrors({})
    }
  }, [isOpen, mode, existingRecord, columns])

  const handleInputChange = (columnName: string, value: unknown) => {
    setFormData(prev => ({
      ...prev,
      [columnName]: value
    }))

    if (errors[columnName]) {
      setErrors(prev => {
        const newErrors = {...prev}
        delete newErrors[columnName]
        return newErrors
      })
    }
  }

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {}

    columns.forEach(column => {
      const value = formData[column.columnName]

      if (!column.nullable && (value === null || value === undefined || value === '')) {
        newErrors[column.columnName] = t('recordEdit.requiredField')
      }

      if (column.autoIncrement && mode === 'create' && value !== null && value !== undefined && value !== '') {
        newErrors[column.columnName] = t('recordEdit.autoIncrementField')
      }
    })

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm()) {
      return
    }

    try {
      setLoading(true)

      if (mode === 'create') {
        const createData: RecordCreateData = {
          data: formData
        }

        await dataAccessService.createRecord(connectionId, schemaName, tableName, createData)
        onSuccess(t('recordEdit.createSuccess'))
      } else {
        const primaryKeyColumns = columns.filter(col => col.primaryKey)
        const whereConditions: Record<string, unknown> = {}

        primaryKeyColumns.forEach(col => {
          whereConditions[col.columnName] = existingRecord?.[col.columnName]
        })

        const updateData: RecordUpdateData = {
          updateData: formData,
          whereConditions
        }

        await dataAccessService.updateRecord(connectionId, schemaName, tableName, updateData)
        onSuccess(t('recordEdit.updateSuccess'))
      }

      onClose()
    } catch (err) {
      console.error('Error saving record:', err)
      onError(err instanceof Error ? err.message : t('recordEdit.saveError'))
    } finally {
      setLoading(false)
    }
  }

  const getInputType = (column: AccessibleColumn): string => {
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

    if (dataType.includes('bool')) {
      return 'checkbox'
    }

    return 'text'
  }

  const renderInput = (column: AccessibleColumn) => {
    const inputType = getInputType(column)
    const value = formData[column.columnName]?.toString() ?? ''

    if (inputType === 'checkbox') {
      return (
        <input
          type="checkbox"
          id={column.columnName}
          checked={Boolean(value)}
          onChange={(e) => handleInputChange(column.columnName, e.target.checked)}
          disabled={loading}
        />
      )
    }

    if (column.columnSize && column.columnSize > 255) {
      return (
        <textarea
          id={column.columnName}
          value={value}
          onChange={(e) => handleInputChange(column.columnName, e.target.value)}
          disabled={loading || (column.autoIncrement && mode === 'create')}
          rows={4}
        />
      )
    }

    return (
      <input
        type={inputType}
        id={column.columnName}
        value={value}
        onChange={(e) => handleInputChange(column.columnName, e.target.value)}
        disabled={loading || (column.autoIncrement && mode === 'create')}
        step={inputType === 'number' && column.decimalDigits ? '0.01' : undefined}
      />
    )
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-content record-edit-modal">
        <div className="modal-header">
          <h3>
            {mode === 'create'
              ? t('recordEdit.createTitle', {tableName})
              : t('recordEdit.editTitle', {tableName})
            }
          </h3>
          <button type="button" className="modal-close" onClick={onClose}>×</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-fields">
            {columns.map(column => (
              <div key={column.columnName} className="form-field">
                <label htmlFor={column.columnName} className="form-label">
                  <span className="field-name">{column.columnName}</span>
                  <span className="field-info">
                    <span className="data-type">{column.dataType}</span>
                    {column.primaryKey && <span className="pk-badge">PK</span>}
                    {!column.nullable && <span className="required-badge">*</span>}
                    {column.autoIncrement && <span className="auto-inc-badge">AUTO</span>}
                  </span>
                </label>

                {renderInput(column)}

                {errors[column.columnName] && (
                  <span className="field-error">{errors[column.columnName]}</span>
                )}

                {column.comment && (
                  <span className="field-comment">{column.comment}</span>
                )}
              </div>
            ))}
          </div>
        </form>

        <div className="modal-footer">
          <button
            type="button"
            className="button button-secondary"
            onClick={onClose}
            disabled={loading}
          >
            {t('common.cancel')}
          </button>
          <button
            type="submit"
            className="button button-primary"
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? t('common.saving') : (mode === 'create' ? t('common.create') : t('common.update'))}
          </button>
        </div>
      </div>
    </div>
  )
}

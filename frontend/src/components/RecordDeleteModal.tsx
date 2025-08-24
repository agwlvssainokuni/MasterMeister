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
import {dataAccessService} from '../services/dataAccessService'
import type {ColumnMetadata, RecordDeleteData, TableRecord} from '../types/frontend'
import '../styles/components/Modal.css'

interface RecordDeleteModalProps {
  isOpen: boolean
  connectionId: number
  schemaName: string
  tableName: string
  record: TableRecord
  columns: ColumnMetadata[]
  onClose: () => void
  onSuccess: (message: string) => void
  onError: (error: string) => void
}

export const RecordDeleteModal: React.FC<RecordDeleteModalProps> = (
  {isOpen, connectionId, schemaName, tableName, record, columns, onClose, onSuccess, onError}
) => {
  const {t} = useTranslation()
  const [loading, setLoading] = useState(false)

  const handleDelete = async () => {
    try {
      setLoading(true)

      const primaryKeyColumns = columns.filter(col => col.primaryKey)
      const whereConditions: Record<string, unknown> = {}

      if (primaryKeyColumns.length > 0) {
        primaryKeyColumns.forEach(col => {
          whereConditions[col.columnName] = record[col.columnName]
        })
      } else {
        Object.keys(record).forEach(key => {
          whereConditions[key] = record[key]
        })
      }

      const deleteData: RecordDeleteData = {
        whereConditions
      }

      await dataAccessService.deleteRecord(connectionId, schemaName, tableName, deleteData)
      onSuccess(t('recordDelete.success'))
      onClose()
    } catch (err) {
      console.error('Error deleting record:', err)
      onError(err instanceof Error ? err.message : t('recordDelete.error'))
    } finally {
      setLoading(false)
    }
  }

  const formatValue = (value: unknown): string => {
    if (value === null || value === undefined) return 'NULL'
    if (typeof value === 'boolean') return value ? 'true' : 'false'
    return String(value)
  }

  const getPrimaryKeyInfo = () => {
    const primaryKeyColumns = columns.filter(col => col.primaryKey)
    if (primaryKeyColumns.length === 0) {
      return t('recordDelete.noPrimaryKey')
    }

    return primaryKeyColumns
      .map(col => `${col.columnName}: ${formatValue(record[col.columnName])}`)
      .join(', ')
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-content record-delete-modal">
        <div className="modal-header">
          <h3>{t('recordDelete.title')}</h3>
          <button type="button" className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">
          <div className="warning-message">
            <div className="warning-icon">⚠️</div>
            <p>{t('recordDelete.warning')}</p>
          </div>

          <div className="delete-details">
            <div className="table-info">
              <strong>{t('recordDelete.table')}:</strong> {`${schemaName}.${tableName}`}
            </div>

            <div className="record-info">
              <strong>{t('recordDelete.record')}:</strong>
              <div className="primary-key-info">
                {getPrimaryKeyInfo()}
              </div>
            </div>

            <div className="record-preview">
              <strong>{t('recordDelete.recordData')}:</strong>
              <div className="record-data">
                {Object.entries(record).slice(0, 5).map(([key, value]) => (
                  <div key={key} className="record-field">
                    <span className="field-name">{key}:</span>
                    <span className="field-value">{formatValue(value)}</span>
                  </div>
                ))}
                {Object.keys(record).length > 5 && (
                  <div className="more-fields">
                    {t('recordDelete.moreFields', {count: Object.keys(record).length - 5})}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="confirmation-text">
            {t('recordDelete.confirmText')}
          </div>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
            disabled={loading}
          >
            {t('common.cancel')}
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleDelete}
            disabled={loading}
          >
            {loading ? t('common.deleting') : t('common.delete')}
          </button>
        </div>
      </div>
    </div>
  )
}

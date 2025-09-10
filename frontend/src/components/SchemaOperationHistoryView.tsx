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
import {FaBook, FaCog, FaDownload, FaHistory, FaInfoCircle, FaSyncAlt, FaTimes, FaUpload} from 'react-icons/fa'
import type {Database, SchemaUpdateLog} from '../types/frontend'

interface SchemaOperationHistoryViewProps {
  connection: Database
  operationHistory: SchemaUpdateLog[]
  onRefresh: () => void
}

export const SchemaOperationHistoryView: React.FC<SchemaOperationHistoryViewProps> = ({
                                                                                        operationHistory,
                                                                                        onRefresh
                                                                                      }) => {
  const {t} = useTranslation()
  const [showFailedOnly, setShowFailedOnly] = useState(false)
  const [selectedLog, setSelectedLog] = useState<SchemaUpdateLog | null>(null)

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).format(date)
  }

  const formatDuration = (ms: number) => {
    if (ms < 1000) return `${ms}ms`
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
    return `${(ms / 60000).toFixed(1)}m`
  }

  const getOperationLabel = (operation: string) => {
    switch (operation) {
      case 'READ_SCHEMA':
        return t('schema.operations.readSchema')
      case 'REFRESH_SCHEMA':
        return t('schema.operations.refreshSchema')
      case 'IMPORT_SCHEMA':
        return t('schema.operations.importSchema')
      case 'EXPORT_SCHEMA':
        return t('schema.operations.exportSchema')
      default:
        return operation
    }
  }

  const getOperationIcon = (operation: string, success: boolean) => {
    if (!success) return <FaTimes/>

    switch (operation) {
      case 'READ_SCHEMA':
        return <FaBook/>
      case 'REFRESH_SCHEMA':
        return <FaSyncAlt/>
      case 'IMPORT_SCHEMA':
        return <FaUpload/>
      case 'EXPORT_SCHEMA':
        return <FaDownload/>
      default:
        return <FaCog/>
    }
  }

  const handleShowDetails = (log: SchemaUpdateLog) => {
    setSelectedLog(log)
  }

  const handleCloseDetails = () => {
    setSelectedLog(null)
  }

  const filteredHistory = showFailedOnly
    ? operationHistory.filter(log => !log.success)
    : operationHistory

  const successCount = operationHistory.filter(log => log.success).length
  const failureCount = operationHistory.length - successCount

  return (
    <div className="schema-operation-history">
      <div className="history-header">
        <div className="history-info">
          <h3>{t('schema.operationHistory')}</h3>
          <div className="history-stats">
            <span className="stat-success">
              {t('schema.successfulOperations', {count: successCount})}
            </span>
            <span className="stat-failure">
              {t('schema.failedOperations', {count: failureCount})}
            </span>
          </div>
        </div>

        <div className="history-actions">
          <label className="filter-checkbox">
            <input
              type="checkbox"
              checked={showFailedOnly}
              onChange={(e) => setShowFailedOnly(e.target.checked)}
            />
            <span className="checkbox-label">
              {t('schema.showFailedOnly')}
            </span>
          </label>
          <button
            className="button button-secondary button-sm"
            onClick={onRefresh}
          >
            {t('common.refresh')}
          </button>
        </div>
      </div>

      {filteredHistory.length === 0 ? (
        <div className="empty-history">
          <div className="empty-icon">
            <FaHistory/>
          </div>
          <h4>{showFailedOnly ? t('schema.noFailedOperations') : t('schema.noOperations')}</h4>
          <p>
            {showFailedOnly
              ? t('schema.noFailedOperationsDescription')
              : t('schema.noOperationsDescription')
            }
          </p>
        </div>
      ) : (
        <div className="table-container">
          <table className="table history-table table-striped">
            <thead className="history-table-header">
            <tr>
              <th className="history-status-header">{t('common.status')}</th>
              <th className="history-operation-header">{t('schema.operation')}</th>
              <th className="history-user-header">{t('schema.user')}</th>
              <th className="history-time-header">{t('schema.time')}</th>
              <th className="history-duration-header">{t('schema.duration')}</th>
              <th className="history-tables-header">{t('schema.tables')}</th>
              <th className="history-actions-header">{t('common.actions')}</th>
            </tr>
            </thead>
            <tbody className="history-table-body">
            {filteredHistory.map((log) => (
              <tr key={log.id} className={log.success ? 'success' : 'failure'}>
                <td className="status-cell">
                    <span className="operation-icon">
                      {getOperationIcon(log.operation, log.success)}
                    </span>
                </td>
                <td className="operation-cell">
                  {getOperationLabel(log.operation)}
                </td>
                <td className="user-cell">
                  {log.userEmail}
                </td>
                <td className="time-cell">
                  {formatDate(log.createdAt)}
                </td>
                <td className="duration-cell">
                  {formatDuration(log.executionTimeMs)}
                </td>
                <td className="tables-cell">
                  {log.tablesCount !== undefined ? log.tablesCount : '—'}
                </td>
                <td className="actions-cell">
                  <button
                    type="button"
                    className="button button-sm button-secondary"
                    onClick={() => handleShowDetails(log)}
                    title={t('schema.viewDetails')}
                  >
                    <FaInfoCircle/>
                  </button>
                </td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 詳細ポップアップ */}
      {selectedLog && (
        <div className="modal-overlay open" onClick={handleCloseDetails}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">{t('schema.operationDetails')}</h3>
              <button
                type="button"
                className="modal-close"
                onClick={handleCloseDetails}
              >
                <FaTimes/>
              </button>
            </div>

            <div className="modal-body">
              <div className="operation-summary">
                <div className="summary-row">
                  <span className="summary-label">{t('schema.operationId')}</span>
                  <span className="summary-value">{selectedLog.id}</span>
                </div>
                <div className="summary-row">
                  <span className="summary-label">{t('schema.operation')}</span>
                  <span className="summary-value">
                    <span className="operation-icon">
                      {getOperationIcon(selectedLog.operation, selectedLog.success)}
                    </span>
                    {getOperationLabel(selectedLog.operation)}
                  </span>
                </div>
                <div className="summary-row">
                  <span className="summary-label">{t('schema.user')}</span>
                  <span className="summary-value">{selectedLog.userEmail}</span>
                </div>
                <div className="summary-row">
                  <span className="summary-label">{t('schema.executionTime')}</span>
                  <span className="summary-value">{formatDate(selectedLog.createdAt)}</span>
                </div>
                <div className="summary-row">
                  <span className="summary-label">{t('schema.duration')}</span>
                  <span className="summary-value">{formatDuration(selectedLog.executionTimeMs)}</span>
                </div>
                <div className="summary-row">
                  <span className="summary-label">{t('common.status')}</span>
                  <span className={`summary-value status-${selectedLog.success ? 'success' : 'failure'}`}>
                    {selectedLog.success ? t('common.success') : t('common.error')}
                  </span>
                </div>
                {selectedLog.tablesCount !== undefined && (
                  <div className="summary-row">
                    <span className="summary-label">{t('schema.tablesProcessed')}</span>
                    <span className="summary-value">{selectedLog.tablesCount}</span>
                  </div>
                )}
                {selectedLog.columnsCount !== undefined && (
                  <div className="summary-row">
                    <span className="summary-label">{t('schema.columnsProcessed')}</span>
                    <span className="summary-value">{selectedLog.columnsCount}</span>
                  </div>
                )}
              </div>

              {selectedLog.details && (
                <div className="operation-details-section">
                  <h4>{t('schema.operationDetails')}</h4>
                  <pre className="operation-details-text">{selectedLog.details}</pre>
                </div>
              )}

              {selectedLog.errorMessage && (
                <div className="error-details-section">
                  <h4>{t('schema.errorDetails')}</h4>
                  <pre className="error-message-text">{selectedLog.errorMessage}</pre>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

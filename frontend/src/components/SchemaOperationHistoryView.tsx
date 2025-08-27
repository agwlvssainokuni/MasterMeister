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

import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Database, SchemaUpdateLog } from '../types/frontend'

interface SchemaOperationHistoryViewProps {
  connection: Database
  operationHistory: SchemaUpdateLog[]
  onRefresh: () => void
}

export const SchemaOperationHistoryView: React.FC<SchemaOperationHistoryViewProps> = ({
  operationHistory,
  onRefresh
}) => {
  const { t } = useTranslation()
  const [showFailedOnly, setShowFailedOnly] = useState(false)
  const [expandedLogs, setExpandedLogs] = useState<Set<number>>(new Set())

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
      case 'READ_SCHEMA': return t('schema.operations.readSchema')
      case 'REFRESH_SCHEMA': return t('schema.operations.refreshSchema')
      case 'IMPORT_SCHEMA': return t('schema.operations.importSchema')
      case 'EXPORT_SCHEMA': return t('schema.operations.exportSchema')
      default: return operation
    }
  }

  const getOperationIcon = (operation: string, success: boolean) => {
    if (!success) return '❌'
    
    switch (operation) {
      case 'READ_SCHEMA': return '📖'
      case 'REFRESH_SCHEMA': return '🔄'
      case 'IMPORT_SCHEMA': return '📥'
      case 'EXPORT_SCHEMA': return '📤'
      default: return '⚙️'
    }
  }

  const toggleLogExpansion = (logId: number) => {
    const newExpanded = new Set(expandedLogs)
    if (newExpanded.has(logId)) {
      newExpanded.delete(logId)
    } else {
      newExpanded.add(logId)
    }
    setExpandedLogs(newExpanded)
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
              {t('schema.successfulOperations', { count: successCount })}
            </span>
            <span className="stat-failure">
              {t('schema.failedOperations', { count: failureCount })}
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
          <div className="empty-icon">📋</div>
          <h4>{showFailedOnly ? t('schema.noFailedOperations') : t('schema.noOperations')}</h4>
          <p>
            {showFailedOnly 
              ? t('schema.noFailedOperationsDescription')
              : t('schema.noOperationsDescription')
            }
          </p>
        </div>
      ) : (
        <div className="history-list">
          {filteredHistory.map(log => {
            const isExpanded = expandedLogs.has(log.id)
            
            return (
              <div
                key={log.id}
                className={`history-item ${log.success ? 'success' : 'failure'}`}
              >
                <div 
                  className="history-item-header"
                  onClick={() => toggleLogExpansion(log.id)}
                >
                  <div className="operation-info">
                    <span className="operation-icon">
                      {getOperationIcon(log.operation, log.success)}
                    </span>
                    <div className="operation-details">
                      <span className="operation-name">
                        {getOperationLabel(log.operation)}
                      </span>
                      <span className="operation-user">
                        {t('schema.operationBy', { user: log.userEmail })}
                      </span>
                    </div>
                  </div>
                  
                  <div className="operation-meta">
                    <span className="operation-time">
                      {formatDate(log.createdAt)}
                    </span>
                    <span className="operation-duration">
                      {formatDuration(log.executionTimeMs)}
                    </span>
                    {log.tablesCount !== undefined && (
                      <span className="operation-stats">
                        {t('schema.tablesProcessed', { count: log.tablesCount })}
                      </span>
                    )}
                    <span className="expand-icon">
                      {isExpanded ? '▼' : '▶'}
                    </span>
                  </div>
                </div>
                
                {isExpanded && (
                  <div className="history-item-details">
                    {log.errorMessage && (
                      <div className="error-details">
                        <h5>{t('schema.errorDetails')}</h5>
                        <pre className="error-message">{log.errorMessage}</pre>
                      </div>
                    )}
                    
                    {log.details && (
                      <div className="operation-details-section">
                        <h5>{t('schema.operationDetails')}</h5>
                        <pre className="operation-details-text">{log.details}</pre>
                      </div>
                    )}
                    
                    <div className="operation-metadata">
                      <div className="metadata-row">
                        <span className="metadata-label">{t('schema.operationId')}</span>
                        <span className="metadata-value">{log.id}</span>
                      </div>
                      <div className="metadata-row">
                        <span className="metadata-label">{t('schema.executionTime')}</span>
                        <span className="metadata-value">{log.executionTimeMs}ms</span>
                      </div>
                      {log.tablesCount !== undefined && (
                        <div className="metadata-row">
                          <span className="metadata-label">{t('schema.tablesProcessed')}</span>
                          <span className="metadata-value">{log.tablesCount}</span>
                        </div>
                      )}
                      {log.columnsCount !== undefined && (
                        <div className="metadata-row">
                          <span className="metadata-label">{t('schema.columnsProcessed')}</span>
                          <span className="metadata-value">{log.columnsCount}</span>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
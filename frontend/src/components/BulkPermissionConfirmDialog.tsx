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
import type {BulkPermissionOptions, BulkPermissionType} from '../types/frontend'

interface BulkPermissionConfirmDialogProps {
  isOpen: boolean
  permissionType: BulkPermissionType
  options: BulkPermissionOptions
  connectionName: string
  onConfirm: () => void
  onCancel: () => void
}

export const BulkPermissionConfirmDialog: React.FC<BulkPermissionConfirmDialogProps> = (
  {
    isOpen,
    permissionType,
    options,
    connectionName,
    onConfirm,
    onCancel,
  }
) => {
  const {t} = useTranslation()

  if (!isOpen) {
    return null
  }

  const isWritePermission = permissionType === 'write' || permissionType === 'delete'
  const userCount = options.userEmails.length || 'all active'

  return (
    <div className={`modal-overlay ${isOpen ? 'open' : ''}`}>
      <div className="modal modal-content bulk-permission-confirm-dialog">
        <div className="modal-header">
          <h3 className="modal-title">
            {isWritePermission && <span className="warning-icon">⚠️</span>}
            {t('permissions.confirmDialog.title')}
          </h3>
        </div>

        <div className="modal-body">
          <div className="confirmation-details">
            <div className="detail-row">
              <span className="detail-label">{t('permissions.confirmDialog.connection')}:</span>
              <span className="detail-value">{connectionName}</span>
            </div>

            <div className="detail-row">
              <span className="detail-label">{t('permissions.confirmDialog.permissionType')}:</span>
              <span className={`detail-value permission-type-${permissionType}`}>
                {t(`permissions.types.${permissionType}` as const)}
              </span>
            </div>

            <div className="detail-row">
              <span className="detail-label">{t('permissions.confirmDialog.scope')}:</span>
              <span className="detail-value">
                {t(`permissions.scopes.${options.scope}`)}
              </span>
            </div>

            <div className="detail-row">
              <span className="detail-label">{t('permissions.confirmDialog.users')}:</span>
              <span className="detail-value">
                {typeof userCount === 'number' ?
                  t('permissions.confirmDialog.userCount', {count: userCount}) :
                  t('permissions.confirmDialog.allActiveUsers')
                }
              </span>
            </div>

            {options.includeSystemTables && (
              <div className="detail-row">
                <span className="detail-label">{t('permissions.confirmDialog.systemTables')}:</span>
                <span className="detail-value included">
                  {t('permissions.confirmDialog.included')}
                </span>
              </div>
            )}
          </div>

          {isWritePermission && (
            <div className="security-warning">
              <div className="warning-header">
                <span className="warning-icon">🔐</span>
                <h4>{t('permissions.confirmDialog.securityWarning')}</h4>
              </div>
              <p>{t(`permissions.confirmDialog.${permissionType}Warning`)}</p>
              <ul>
                <li>{t('permissions.confirmDialog.warnings.dataModification')}</li>
                <li>{t('permissions.confirmDialog.warnings.auditLogging')}</li>
                <li>{t('permissions.confirmDialog.warnings.irreversible')}</li>
              </ul>
            </div>
          )}

          <div className="confirmation-message">
            <p>
              <strong>
                {t('permissions.confirmDialog.confirmMessage', {
                  type: t(`permissions.types.${permissionType}` as const),
                  connection: connectionName
                })}
              </strong>
            </p>
          </div>
        </div>

        <div className="modal-footer">
          <button
            className="button button-secondary"
            onClick={onCancel}
          >
            {t('common.cancel')}
          </button>
          <button
            className={`button ${isWritePermission ? 'button-warning' : 'button-primary'}`}
            onClick={onConfirm}
          >
            {isWritePermission && <span className="button-icon">⚠️</span>}
            {t('permissions.confirmDialog.confirmButton')}
          </button>
        </div>
      </div>
    </div>
  )
}
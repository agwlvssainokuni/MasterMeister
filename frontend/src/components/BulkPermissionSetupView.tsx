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
import type {
  BulkPermissionOptions,
  BulkPermissionResult,
  BulkPermissionScope,
  BulkPermissionType,
  Database
} from '../types/frontend'

interface BulkPermissionSetupViewProps {
  connection: Database
  loading: boolean
  onBulkGrant: (connectionId: number, options: BulkPermissionOptions) => Promise<BulkPermissionResult>
  onShowConfirmDialog: (
    type: BulkPermissionType,
    options: BulkPermissionOptions,
    onConfirm: () => void
  ) => void
}

export const BulkPermissionSetupView: React.FC<BulkPermissionSetupViewProps> = (
  {
    connection,
    loading,
    onBulkGrant,
    onShowConfirmDialog,
  }
) => {
  const {t} = useTranslation()

  const [selectedType, setSelectedType] = useState<BulkPermissionType>('read' as const)
  const [selectedScope, setSelectedScope] = useState<BulkPermissionScope>('ALL_TABLES')
  const [userEmails, setUserEmails] = useState('')
  const [description, setDescription] = useState('')
  const [includeSystemTables, setIncludeSystemTables] = useState(false)
  const [bulkResult, setBulkResult] = useState<BulkPermissionResult | null>(null)


  const handleCustomSetup = () => {
    const emails = userEmails.split(',').map(email => email.trim()).filter(email => email.length > 0)

    if (emails.length === 0) {
      return
    }

    const options: BulkPermissionOptions = {
      scope: selectedScope,
      permissionType: selectedType,
      userEmails: emails,
      includeSystemTables,
      description: description || t(`permissions.bulkSetup.defaultDescription.${selectedType}`)
    }

    onShowConfirmDialog(selectedType, options, async () => {
      try {
        const result = await onBulkGrant(connection.id, options)
        setBulkResult(result)
      } catch (error) {
        console.error('Custom bulk permission setup failed:', error)
      }
    })
  }

  return (
    <div className="bulk-permission-setup">
      <div className="bulk-setup-section">
        <p className="section-description">
          {t('permissions.bulkSetupDescription')}
        </p>

        <div className="setup-form">
          <div className="form-row">
            <div className="form-group">
              <label>{t('permissions.bulkSetup.permissionType')}</label>
              <select
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value as BulkPermissionType)}
                className="form-select"
              >
                <option value="read">{t('permissions.types.read')}</option>
                <option value="write">{t('permissions.types.write')}</option>
                <option value="delete">{t('permissions.types.delete')}</option>
              </select>
            </div>

            <div className="form-group">
              <label>{t('permissions.bulkSetup.scope')}</label>
              <select
                value={selectedScope}
                onChange={(e) => setSelectedScope(e.target.value as BulkPermissionScope)}
                className="form-select"
              >
                <option value="ALL_TABLES">{t('permissions.scopes.allTables')}</option>
                <option value="SCHEMA">{t('permissions.scopes.schema')}</option>
                <option value="TABLE_LIST">{t('permissions.scopes.tableList')}</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="user-emails">{t('permissions.bulkSetup.userEmails')}</label>
            <textarea
              id="user-emails"
              value={userEmails}
              onChange={(e) => setUserEmails(e.target.value)}
              placeholder={t('permissions.bulkSetup.userEmailsPlaceholder')}
              className="form-textarea"
              rows={3}
            />
            <small className="form-hint">
              {t('permissions.bulkSetup.userEmailsHint')}
            </small>
          </div>

          <div className="form-group">
            <label htmlFor="description">{t('permissions.bulkSetup.description')}</label>
            <input
              id="description"
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('permissions.bulkSetup.descriptionPlaceholder')}
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={includeSystemTables}
                onChange={(e) => setIncludeSystemTables(e.target.checked)}
              />
              <span>{t('permissions.bulkSetup.includeSystemTables')}</span>
            </label>
          </div>

          <div className="form-actions">
            <button
              className="button button-primary"
              onClick={handleCustomSetup}
              disabled={loading || userEmails.trim().length === 0}
            >
              {loading ? t('permissions.applying') : t('permissions.bulkSetup.apply')}
            </button>
          </div>
        </div>
      </div>

      {bulkResult && (
        <div className="bulk-result">
          <h5>{t('permissions.bulkSetup.result')}</h5>
          <div className="result-stats">
            <div className="stat-item">
              <span className="stat-value">{bulkResult.processedUsers}</span>
              <span className="stat-label">{t('permissions.bulkResult.processedUsers')}</span>
            </div>
            <div className="stat-item">
              <span className="stat-value">{bulkResult.processedTables}</span>
              <span className="stat-label">{t('permissions.bulkResult.processedTables')}</span>
            </div>
            <div className="stat-item">
              <span className="stat-value">{bulkResult.createdPermissions}</span>
              <span className="stat-label">{t('permissions.bulkResult.createdPermissions')}</span>
            </div>
            <div className="stat-item">
              <span className="stat-value">{bulkResult.skippedExisting}</span>
              <span className="stat-label">{t('permissions.bulkResult.skippedExisting')}</span>
            </div>
          </div>
          {bulkResult.errors.length > 0 && (
            <div className="result-errors">
              <h6>{t('permissions.bulkResult.errors')}</h6>
              <ul>
                {bulkResult.errors.map((error, index) => (
                  <li key={index}>{error}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
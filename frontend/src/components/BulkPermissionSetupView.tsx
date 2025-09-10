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
    types: BulkPermissionType[],
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

  const [selectedTypes, setSelectedTypes] = useState<BulkPermissionType[]>(['read'])
  const [selectedScope, setSelectedScope] = useState<BulkPermissionScope>('CONNECTION')
  const [userEmails, setUserEmails] = useState('')
  const [schemaNames, setSchemaNames] = useState('')
  const [tableNames, setTableNames] = useState('')
  const [description, setDescription] = useState('')
  const [includeSystemTables, setIncludeSystemTables] = useState(false)
  const [bulkResult, setBulkResult] = useState<BulkPermissionResult | null>(null)


  const handleCustomSetup = () => {
    const emails = userEmails.split(',').map(email => email.trim()).filter(email => email.length > 0)
    const schemas = schemaNames.split(',').map(s => s.trim()).filter(s => s.length > 0)
    const tables = tableNames.split(',').map(t => t.trim()).filter(t => t.length > 0)

    // Validation based on scope
    if (selectedScope === 'SCHEMA' && schemas.length === 0) {
      return
    }
    if (selectedScope === 'TABLE' && tables.length === 0) {
      return
    }
    if (selectedTypes.length === 0) {
      return
    }

    const options: BulkPermissionOptions = {
      scope: selectedScope,
      permissionTypes: selectedTypes,
      userEmails: emails,
      schemaNames: schemas.length > 0 ? schemas : undefined,
      tableNames: tables.length > 0 ? tables : undefined,
      includeSystemTables,
      description: description || `Bulk ${selectedTypes.join(', ')} permissions applied`
    }

    onShowConfirmDialog(selectedTypes, options, async () => {
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
          <div className="form-group">
            <label>{t('permissions.bulkSetup.permissionType')}</label>
            <div className="checkbox-group">
              {(['read', 'write', 'delete'] as BulkPermissionType[]).map(type => (
                <label key={type} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={selectedTypes.includes(type)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedTypes([...selectedTypes, type])
                      } else {
                        setSelectedTypes(selectedTypes.filter(t => t !== type))
                      }
                    }}
                  />
                  <span>{t(`permissions.types.${type}`)}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label>{t('permissions.bulkSetup.scope')}</label>
            <select
              value={selectedScope}
              onChange={(e) => setSelectedScope(e.target.value as BulkPermissionScope)}
              className="form-select"
            >
              <option value="CONNECTION">{t('permissions.scopes.connection')}</option>
              <option value="SCHEMA">{t('permissions.scopes.schema')}</option>
              <option value="TABLE">{t('permissions.scopes.table')}</option>
            </select>
          </div>

          {selectedScope === 'SCHEMA' && (
            <div className="form-group">
              <label htmlFor="schema-names">Schema Names</label>
              <textarea
                id="schema-names"
                value={schemaNames}
                onChange={(e) => setSchemaNames(e.target.value)}
                placeholder="schema1, schema2, schema3"
                className="form-textarea"
                rows={2}
              />
              <small className="form-hint">
                Enter schema names separated by commas
              </small>
            </div>
          )}

          {selectedScope === 'TABLE' && (
            <div className="form-group">
              <label htmlFor="table-names">Table Names</label>
              <textarea
                id="table-names"
                value={tableNames}
                onChange={(e) => setTableNames(e.target.value)}
                placeholder="schema1.table1, schema2.table2"
                className="form-textarea"
                rows={3}
              />
              <small className="form-hint">
                Enter full table names (schema.table) separated by commas
              </small>
            </div>
          )}

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
              disabled={loading || selectedTypes.length === 0 || 
                       (selectedScope === 'SCHEMA' && schemaNames.trim().length === 0) ||
                       (selectedScope === 'TABLE' && tableNames.trim().length === 0)}
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
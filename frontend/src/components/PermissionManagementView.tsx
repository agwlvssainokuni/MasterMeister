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

import React, {useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {BulkPermissionSetupView} from './BulkPermissionSetupView'
import {BulkPermissionConfirmModal} from './BulkPermissionConfirmModal'
import type {
  BulkPermissionOptions,
  BulkPermissionResponse,
  BulkPermissionType,
  Database,
  PermissionImportOptions,
  PermissionImportResponse,
  PermissionValidationResponse
} from '../types/frontend'

interface PermissionManagementViewProps {
  connection: Database
  loading: boolean
  onExport: (connectionId: number, description?: string) => Promise<void>
  onImport: (connectionId: number, file: File, options: PermissionImportOptions) => Promise<PermissionImportResponse>
  onValidate: (connectionId: number, file: File) => Promise<PermissionValidationResponse>
  onBulkGrant?: (connectionId: number, options: BulkPermissionOptions) => Promise<BulkPermissionResponse>
}

export const PermissionManagementView: React.FC<PermissionManagementViewProps> = (
  {
    connection,
    loading,
    onExport,
    onImport,
    onValidate,
    onBulkGrant,
  }
) => {
  const {t} = useTranslation()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [activeTab, setActiveTab] = useState<'quickSetup' | 'export' | 'import'>('quickSetup')
  const [exportDescription, setExportDescription] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [validationResult, setValidationResult] = useState<PermissionValidationResponse | null>(null)
  const [importResult, setImportResult] = useState<PermissionImportResponse | null>(null)
  const [importOptions, setImportOptions] = useState<PermissionImportOptions>({
    importUsers: true,
    importTemplates: true,
    clearExistingPermissions: false,
    duplicateHandling: 'overwrite'
  })
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)
  const [confirmOptions, setConfirmOptions] = useState<{
    type: BulkPermissionType
    options: BulkPermissionOptions
    onConfirm: () => void
  } | null>(null)

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      setValidationResult(null)
      setImportResult(null)
    }
  }

  const handleExport = async () => {
    await onExport(connection.id, exportDescription || undefined)
  }

  const handleValidate = async () => {
    if (!selectedFile) return

    try {
      const result = await onValidate(connection.id, selectedFile)
      setValidationResult(result)
    } catch {
      setValidationResult(null)
    }
  }

  const handleImport = async () => {
    if (!selectedFile) return

    try {
      const result = await onImport(connection.id, selectedFile, importOptions)
      setImportResult(result)
    } catch (error) {
      // エラーが発生した場合、詳細な情報を保持してUIに表示
      console.error('Import operation failed:', error)
      setImportResult({
        importedUsers: 0,
        importedTemplates: 0,
        importedPermissions: 0,
        updatedPermissions: 0,
        skippedDuplicates: 0,
        warnings: [],
        errors: [error instanceof Error ? error.message : 'Unknown import error occurred']
      })
    }
  }

  const clearFile = () => {
    setSelectedFile(null)
    setValidationResult(null)
    setImportResult(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleShowConfirmDialog = (
    type: BulkPermissionType,
    options: BulkPermissionOptions,
    onConfirm: () => void
  ) => {
    setConfirmOptions({type, options, onConfirm})
    setShowConfirmDialog(true)
  }

  const handleConfirmDialogClose = () => {
    setShowConfirmDialog(false)
    setConfirmOptions(null)
  }

  const handleConfirm = () => {
    if (confirmOptions) {
      confirmOptions.onConfirm()
    }
    handleConfirmDialogClose()
  }

  return (
    <div className="permission-manager">
      <div className="manager-header">
        <h3>{connection.name}</h3>
        <p className="connection-info">
          {t('permissions.connectionInfo', {
            host: connection.host,
            port: connection.port,
            type: connection.dbType
          })} - {connection.databaseName}
        </p>
      </div>

      <div className="tab-navigation">
        <button
          className={`tab-button ${activeTab === 'quickSetup' ? 'active' : ''}`}
          onClick={() => setActiveTab('quickSetup')}
        >
          ⚡ {t('permissions.tabs.quickSetup')}
        </button>
        <button
          className={`tab-button ${activeTab === 'export' ? 'active' : ''}`}
          onClick={() => setActiveTab('export')}
        >
          📤 {t('permissions.tabs.export')}
        </button>
        <button
          className={`tab-button ${activeTab === 'import' ? 'active' : ''}`}
          onClick={() => setActiveTab('import')}
        >
          📥 {t('permissions.tabs.import')}
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'quickSetup' && onBulkGrant && (
          <BulkPermissionSetupView
            connection={connection}
            loading={loading}
            onBulkGrant={onBulkGrant}
            onShowConfirmDialog={handleShowConfirmDialog}
          />
        )}

        {activeTab === 'quickSetup' && !onBulkGrant && (
          <div className="feature-notice">
            <span className="notice-icon">⚠️</span>
            <span>{t('permissions.quickSetupNotAvailable')}</span>
          </div>
        )}

        {activeTab === 'export' && (
          <div className="export-section">
            <h4>{t('permissions.exportTitle')}</h4>
            <p className="section-description">
              {t('permissions.exportDescription')}
            </p>

            <div className="export-form">
              <div className="form-group">
                <label htmlFor="export-description">
                  {t('permissions.exportDescriptionLabel')}
                </label>
                <input
                  id="export-description"
                  type="text"
                  value={exportDescription}
                  onChange={(e) => setExportDescription(e.target.value)}
                  placeholder={t('permissions.exportDescriptionPlaceholder')}
                  className="text-input"
                />
                <small className="form-hint">
                  {t('permissions.exportDescriptionHint')}
                </small>
              </div>

              <button
                className="button button-primary"
                onClick={handleExport}
                disabled={loading}
              >
                {loading ? t('permissions.exporting') : t('permissions.actions.export')}
              </button>
            </div>
          </div>
        )}

        {activeTab === 'import' && (
          <div className="import-section">
            <h4>{t('permissions.importTitle')}</h4>
            <p className="section-description">
              {t('permissions.importDescription')}
            </p>

            <div className="file-upload-section">
              <div className="file-input-wrapper">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".yml,.yaml"
                  onChange={handleFileSelect}
                  className="file-input"
                  id="permission-file"
                />
                <label htmlFor="permission-file" className="file-input-label">
                  📄 {selectedFile ? selectedFile.name : t('permissions.selectFile')}
                </label>
                {selectedFile && (
                  <button
                    className="button button-secondary button-sm"
                    onClick={clearFile}
                  >
                    ✕ {t('common.clear')}
                  </button>
                )}
              </div>

              {selectedFile && (
                <div className="file-actions">
                  <button
                    className="button button-secondary"
                    onClick={handleValidate}
                    disabled={loading}
                  >
                    {loading ? t('permissions.validating') : t('permissions.actions.validate')}
                  </button>
                </div>
              )}
            </div>

            {validationResult && (
              <div className={`validation-result ${validationResult.valid ? 'success' : 'error'}`}>
                <div className="result-header">
                  <span className="result-icon">
                    {validationResult.valid ? '✅' : '❌'}
                  </span>
                  <span className="result-message">
                    {validationResult.message}
                  </span>
                </div>
                {validationResult.valid && (
                  <div className="validation-stats">
                    <div className="stat-item">
                      <span className="stat-value">{validationResult.userCount}</span>
                      <span className="stat-label">{t('permissions.userCount')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{validationResult.templateCount}</span>
                      <span className="stat-label">{t('permissions.templateCount')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{validationResult.totalPermissions}</span>
                      <span className="stat-label">{t('permissions.totalPermissions')}</span>
                    </div>
                  </div>
                )}
              </div>
            )}

            {validationResult?.valid && (
              <div className="import-options">
                <h5>{t('permissions.importOptions')}</h5>

                <div className="options-grid">
                  <label className="checkbox-option">
                    <input
                      type="checkbox"
                      checked={importOptions.importUsers}
                      onChange={(e) => setImportOptions(prev => ({
                        ...prev,
                        importUsers: e.target.checked
                      }))}
                    />
                    <span className="checkbox-label">
                      {t('permissions.options.importUsers')}
                    </span>
                  </label>

                  <label className="checkbox-option">
                    <input
                      type="checkbox"
                      checked={importOptions.importTemplates}
                      onChange={(e) => setImportOptions(prev => ({
                        ...prev,
                        importTemplates: e.target.checked
                      }))}
                    />
                    <span className="checkbox-label">
                      {t('permissions.options.importTemplates')}
                    </span>
                  </label>

                  <label className="checkbox-option">
                    <input
                      type="checkbox"
                      checked={importOptions.clearExistingPermissions}
                      onChange={(e) => setImportOptions(prev => ({
                        ...prev,
                        clearExistingPermissions: e.target.checked
                      }))}
                    />
                    <span className="checkbox-label">
                      {t('permissions.options.clearExistingPermissions')}
                    </span>
                  </label>

                  <div className="radio-group">
                    <label className="radio-group-title">{t('permissions.options.duplicateHandling')}</label>
                    <div className="radio-options">
                      <label className="radio-option">
                        <input
                          type="radio"
                          name="duplicateHandling"
                          value="error"
                          checked={importOptions.duplicateHandling === 'error'}
                          onChange={(e) => setImportOptions(prev => ({
                            ...prev,
                            duplicateHandling: e.target.value as 'error' | 'skip' | 'overwrite'
                          }))}
                        />
                        <span>{t('permissions.options.duplicateHandlingError')}</span>
                      </label>
                      <label className="radio-option">
                        <input
                          type="radio"
                          name="duplicateHandling"
                          value="skip"
                          checked={importOptions.duplicateHandling === 'skip'}
                          onChange={(e) => setImportOptions(prev => ({
                            ...prev,
                            duplicateHandling: e.target.value as 'error' | 'skip' | 'overwrite'
                          }))}
                        />
                        <span>{t('permissions.options.duplicateHandlingSkip')}</span>
                      </label>
                      <label className="radio-option">
                        <input
                          type="radio"
                          name="duplicateHandling"
                          value="overwrite"
                          checked={importOptions.duplicateHandling === 'overwrite'}
                          onChange={(e) => setImportOptions(prev => ({
                            ...prev,
                            duplicateHandling: e.target.value as 'error' | 'skip' | 'overwrite'
                          }))}
                        />
                        <span>{t('permissions.options.duplicateHandlingOverwrite')}</span>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="import-actions">
                  <button
                    className="button button-primary"
                    onClick={handleImport}
                    disabled={loading}
                  >
                    {loading ? t('permissions.importing') : t('permissions.actions.import')}
                  </button>
                </div>
              </div>
            )}

            {importResult && (
              <div className={`import-result ${importResult.errors.length > 0 && importResult.importedPermissions === 0 && importResult.updatedPermissions === 0 ? 'error' : 'success'}`}>
                <div className="result-header">
                  <span className="result-icon">
                    {importResult.errors.length > 0 && importResult.importedPermissions === 0 ? '❌' : '✅'}
                  </span>
                  <span className="result-message">
                    {importResult.errors.length > 0 && importResult.importedPermissions === 0 
                      ? t('permissions.importFailed')
                      : t('permissions.importCompleted')
                    }
                  </span>
                </div>
                
                {(importResult.importedPermissions > 0 || importResult.updatedPermissions > 0) && (
                  <div className="result-stats">
                    <div className="stat-item">
                      <span className="stat-value">{importResult.importedUsers}</span>
                      <span className="stat-label">{t('permissions.importedUsers')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{importResult.importedTemplates}</span>
                      <span className="stat-label">{t('permissions.importedTemplates')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{importResult.importedPermissions}</span>
                      <span className="stat-label">{t('permissions.importedPermissions')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{importResult.updatedPermissions}</span>
                      <span className="stat-label">{t('permissions.updatedPermissions')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{importResult.skippedDuplicates}</span>
                      <span className="stat-label">{t('permissions.skippedDuplicates')}</span>
                    </div>
                  </div>
                )}
                
                {importResult.errors.length > 0 && (
                  <div className="import-errors">
                    <h6>{t('permissions.importErrors')}</h6>
                    <ul>
                      {importResult.errors.map((error, index) => (
                        <li key={index}>{error}</li>
                      ))}
                    </ul>
                  </div>
                )}
                
                {importResult.warnings && importResult.warnings.length > 0 && (
                  <div className="import-warnings">
                    <h6>{t('permissions.importWarnings')}</h6>
                    <ul>
                      {importResult.warnings.map((warning, index) => (
                        <li key={index}>{warning}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {showConfirmDialog && confirmOptions && (
        <BulkPermissionConfirmModal
          isOpen={showConfirmDialog}
          permissionType={confirmOptions.type}
          options={confirmOptions.options}
          connectionName={connection.name}
          onConfirm={handleConfirm}
          onCancel={handleConfirmDialogClose}
        />
      )}
    </div>
  )
}
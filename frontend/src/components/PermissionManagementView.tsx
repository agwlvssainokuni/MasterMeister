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
import {IoCheckmarkCircle, IoClose, IoCloseCircle, IoDocument, IoWarning} from 'react-icons/io5'
import {BulkPermissionSetupView} from './BulkPermissionSetupView'
import {BulkPermissionConfirmModal} from './BulkPermissionConfirmModal'
import type {
  BulkPermissionOptions,
  BulkPermissionResult,
  BulkPermissionType,
  Database,
  PermissionImportOptions,
  PermissionImportResult,
  PermissionValidationResult
} from '../types/frontend'

interface PermissionManagementViewProps {
  connection: Database
  loading: boolean
  onExport: (connectionId: number, description?: string) => Promise<void>
  onImport: (connectionId: number, file: File, options: PermissionImportOptions) => Promise<PermissionImportResult>
  onValidate: (connectionId: number, file: File) => Promise<PermissionValidationResult>
  onBulkGrant?: (connectionId: number, options: BulkPermissionOptions) => Promise<BulkPermissionResult>
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

  const [activeTab, setActiveTab] = useState<'bulkSetup' | 'export' | 'import'>('bulkSetup')
  const [exportDescription, setExportDescription] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [validationResult, setValidationResult] = useState<PermissionValidationResult | null>(null)
  const [importResult, setImportResult] = useState<PermissionImportResult | null>(null)
  const [importOptions, setImportOptions] = useState<PermissionImportOptions>({
    importUsers: true,
    importTemplates: true,
    clearExistingPermissions: false,
    duplicateHandling: 'overwrite'
  })
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)
  const [confirmOptions, setConfirmOptions] = useState<{
    types: BulkPermissionType[]
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
    types: BulkPermissionType[],
    options: BulkPermissionOptions,
    onConfirm: () => void
  ) => {
    setConfirmOptions({types, options, onConfirm})
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
      <div className="tabs">
        <ul className="tabs-list">
          <li className={`tab-item ${activeTab === 'bulkSetup' ? 'active' : ''}`}>
            <button
              className="tab-button"
              onClick={() => setActiveTab('bulkSetup')}
            >
              {t('permissions.tabs.bulkSetup')}
            </button>
          </li>
          <li className={`tab-item ${activeTab === 'export' ? 'active' : ''}`}>
            <button
              className="tab-button"
              onClick={() => setActiveTab('export')}
            >
              {t('permissions.tabs.export')}
            </button>
          </li>
          <li className={`tab-item ${activeTab === 'import' ? 'active' : ''}`}>
            <button
              className="tab-button"
              onClick={() => setActiveTab('import')}
            >
              {t('permissions.tabs.import')}
            </button>
          </li>
        </ul>
      </div>

      <div className="tab-content">
        {activeTab === 'bulkSetup' && onBulkGrant && (
          <BulkPermissionSetupView
            connection={connection}
            loading={loading}
            onBulkGrant={onBulkGrant}
            onShowConfirmDialog={handleShowConfirmDialog}
          />
        )}

        {activeTab === 'bulkSetup' && !onBulkGrant && (
          <div className="feature-notice">
            <IoWarning className="notice-icon warning-color"/>
            <span>{t('permissions.quickSetupNotAvailable')}</span>
          </div>
        )}

        {activeTab === 'export' && (
          <div className="export-section">
            <div className="export-form">
              <div className="form-group">
                <label htmlFor="export-description" className="form-label">
                  {t('permissions.exportDescriptionLabel')}
                </label>
                <input
                  id="export-description"
                  type="text"
                  value={exportDescription}
                  onChange={(e) => setExportDescription(e.target.value)}
                  placeholder={t('permissions.exportDescriptionPlaceholder')}
                  className="form-input"
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
                {loading ? t('permissions.exporting') : t('permissions.exportTitle')}
              </button>
            </div>
          </div>
        )}

        {activeTab === 'import' && (
          <div className="import-section">
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
                  <IoDocument className="file-icon"/>
                  {selectedFile ? selectedFile.name : t('permissions.selectFile')}
                </label>
                {selectedFile && (
                  <button
                    className="button button-secondary button-sm"
                    onClick={clearFile}
                  >
                    <IoClose className="clear-icon"/>
                    {t('common.clear')}
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
                  {validationResult.valid ? (
                    <IoCheckmarkCircle className="result-icon success-color"/>
                  ) : (
                    <IoCloseCircle className="result-icon error-color"/>
                  )}
                  <span className="result-message">
                    {validationResult.valid ? t('permissions.validateSuccess') : t('permissions.validateError')}
                  </span>
                </div>
                {validationResult.valid && (
                  <div className="validation-stats">
                    <div className="stat-item">
                      <span className="stat-value">{validationResult.userCount}</span>
                      <span className="stat-label">{t('permissions.userCount')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{validationResult.totalPermissions}</span>
                      <span className="stat-label">{t('permissions.totalPermissions')}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-value">{validationResult.templateCount}</span>
                      <span className="stat-label">{t('permissions.templateCount')}</span>
                    </div>
                  </div>
                )}
                {validationResult.message && (
                  <div className="api-message">
                    {validationResult.message}
                  </div>
                )}
              </div>
            )}

            {validationResult?.valid && (
              <div className="import-options">
                <label className="form-label">{t('permissions.importOptions')}</label>

                <div className="options-grid">
                  <div className="form-group">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={importOptions.importUsers}
                        onChange={(e) => setImportOptions(prev => ({
                          ...prev,
                          importUsers: e.target.checked
                        }))}
                      />
                      <span>{t('permissions.options.importUsers')}</span>
                    </label>
                  </div>

                  <div className="form-group">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={importOptions.importTemplates}
                        onChange={(e) => setImportOptions(prev => ({
                          ...prev,
                          importTemplates: e.target.checked
                        }))}
                      />
                      <span>{t('permissions.options.importTemplates')}</span>
                    </label>
                  </div>

                  <div className="form-group">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={importOptions.clearExistingPermissions}
                        onChange={(e) => setImportOptions(prev => ({
                          ...prev,
                          clearExistingPermissions: e.target.checked
                        }))}
                      />
                      <span>{t('permissions.options.clearExistingPermissions')}</span>
                    </label>
                  </div>

                  <div className="form-group">
                    <label className="form-label">{t('permissions.options.duplicateHandling')}</label>
                    <div className="radio-group">
                      <label className="radio-label">
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
                      <label className="radio-label">
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
                      <label className="radio-label">
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
                    {loading ? t('permissions.importing') : t('permissions.importTitle')}
                  </button>
                </div>
              </div>
            )}

            {importResult && (
              <div
                className={`import-result ${importResult.errors.length > 0 && importResult.importedPermissions === 0 && importResult.updatedPermissions === 0 ? 'error' : 'success'}`}>
                <div className="result-header">
                  {importResult.errors.length > 0 && importResult.importedPermissions === 0 ? (
                    <IoCloseCircle className="result-icon error-color"/>
                  ) : (
                    <IoCheckmarkCircle className="result-icon success-color"/>
                  )}
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
          permissionTypes={confirmOptions.types}
          options={confirmOptions.options}
          connectionName={connection.name}
          onConfirm={handleConfirm}
          onCancel={handleConfirmDialogClose}
        />
      )}
    </div>
  )
}

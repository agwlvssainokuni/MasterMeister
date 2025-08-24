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

import React, { useState, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import type { 
  DatabaseConnection, 
  PermissionImportOptions, 
  PermissionImportResult, 
  PermissionValidationResult 
} from '../types/frontend'

interface PermissionFileManagerProps {
  connection: DatabaseConnection
  loading: boolean
  onExport: (connectionId: number, description?: string) => Promise<void>
  onImport: (connectionId: number, file: File, options: PermissionImportOptions) => Promise<PermissionImportResult>
  onValidate: (connectionId: number, file: File) => Promise<PermissionValidationResult>
}

export const PermissionFileManager: React.FC<PermissionFileManagerProps> = ({
  connection,
  loading,
  onExport,
  onImport,
  onValidate
}) => {
  const { t } = useTranslation()
  const fileInputRef = useRef<HTMLInputElement>(null)
  
  const [activeTab, setActiveTab] = useState<'export' | 'import'>('export')
  const [exportDescription, setExportDescription] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [validationResult, setValidationResult] = useState<PermissionValidationResult | null>(null)
  const [importResult, setImportResult] = useState<PermissionImportResult | null>(null)
  const [importOptions, setImportOptions] = useState<PermissionImportOptions>({
    importUsers: true,
    importTemplates: true,
    clearExistingPermissions: false,
    skipDuplicates: true
  })

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
    } catch {
      setImportResult(null)
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

  return (
    <div className="permission-file-manager">
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

                  <label className="checkbox-option">
                    <input
                      type="checkbox"
                      checked={importOptions.skipDuplicates}
                      onChange={(e) => setImportOptions(prev => ({
                        ...prev,
                        skipDuplicates: e.target.checked
                      }))}
                    />
                    <span className="checkbox-label">
                      {t('permissions.options.skipDuplicates')}
                    </span>
                  </label>
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
              <div className="import-result success">
                <div className="result-header">
                  <span className="result-icon">✅</span>
                  <span className="result-message">
                    {t('permissions.importCompleted')}
                  </span>
                </div>
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
                    <span className="stat-value">{importResult.skippedDuplicates}</span>
                    <span className="stat-label">{t('permissions.skippedDuplicates')}</span>
                  </div>
                </div>
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
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
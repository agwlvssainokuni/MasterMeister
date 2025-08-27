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

import React, { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { Database, DatabaseForm as ConnectionForm, DatabaseType } from '../types/frontend'

interface DatabaseFormViewProps {
  connection?: Database | null
  onSubmit: (formData: ConnectionForm) => void
  onCancel: () => void
}

export const DatabaseFormView: React.FC<DatabaseFormViewProps> = ({
  connection,
  onSubmit,
  onCancel
}) => {
  const { t } = useTranslation()
  const [formData, setFormData] = useState<ConnectionForm>({
    name: '',
    dbType: 'MYSQL' as DatabaseType,
    host: 'localhost',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
    connectionParams: '',
    active: true
  })
  
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (connection) {
      setFormData({
        name: connection.name,
        dbType: connection.dbType,
        host: connection.host,
        port: connection.port,
        databaseName: connection.databaseName,
        username: connection.username,
        password: '', // Don't prefill password for security
        connectionParams: connection.connectionParams || '',
        active: connection.active
      })
    }
  }, [connection])

  const databaseTypes: { value: DatabaseType; label: string; defaultPort: number }[] = [
    { value: 'MYSQL', label: 'MySQL', defaultPort: 3306 },
    { value: 'MARIADB', label: 'MariaDB', defaultPort: 3306 },
    { value: 'POSTGRESQL', label: 'PostgreSQL', defaultPort: 5432 },
    { value: 'H2', label: 'H2', defaultPort: 9092 }
  ]

  const handleInputChange = (field: keyof ConnectionForm, value: string | number | boolean) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }))
    }
  }

  const handleDbTypeChange = (dbType: DatabaseType) => {
    const selectedDb = databaseTypes.find(db => db.value === dbType)
    setFormData(prev => ({
      ...prev,
      dbType,
      port: selectedDb?.defaultPort || prev.port
    }))
  }

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.name.trim()) {
      newErrors.name = t('databaseConnections.validation.nameRequired')
    }
    if (!formData.host.trim()) {
      newErrors.host = t('databaseConnections.validation.hostRequired')
    }
    if (!formData.port || formData.port <= 0 || formData.port > 65535) {
      newErrors.port = t('databaseConnections.validation.portInvalid')
    }
    if (!formData.databaseName.trim()) {
      newErrors.databaseName = t('databaseConnections.validation.databaseNameRequired')
    }
    if (!formData.username.trim()) {
      newErrors.username = t('databaseConnections.validation.usernameRequired')
    }
    if (!formData.password.trim() && !connection) {
      newErrors.password = t('databaseConnections.validation.passwordRequired')
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!validateForm()) {
      return
    }

    try {
      setIsSubmitting(true)
      await onSubmit(formData)
    } catch (error) {
      console.error('Form submission error:', error)
    } finally {
      setIsSubmitting(false)
    }
  }

  const isEditing = !!connection

  return (
    <div className="connection-form-container">
      <div className="form-header">
        <h2>
          {isEditing 
            ? t('databaseConnections.editConnection') 
            : t('databaseConnections.createConnection')
          }
        </h2>
        <p className="form-description">
          {isEditing 
            ? t('databaseConnections.editDescription') 
            : t('databaseConnections.createDescription')
          }
        </p>
      </div>

      <form onSubmit={handleSubmit} className="connection-form">
        <div className="form-grid">
          <div className="form-group">
            <label htmlFor="name" className="form-label">
              {t('databaseConnections.fields.name')} <span className="required">*</span>
            </label>
            <input
              type="text"
              id="name"
              className={`form-input ${errors.name ? 'error' : ''}`}
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              placeholder={t('databaseConnections.placeholders.name')}
            />
            {errors.name && <span className="error-text">{errors.name}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="dbType" className="form-label">
              {t('databaseConnections.fields.dbType')} <span className="required">*</span>
            </label>
            <select
              id="dbType"
              className="form-select"
              value={formData.dbType}
              onChange={(e) => handleDbTypeChange(e.target.value as DatabaseType)}
            >
              {databaseTypes.map(db => (
                <option key={db.value} value={db.value}>
                  {db.label}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="host" className="form-label">
              {t('databaseConnections.fields.host')} <span className="required">*</span>
            </label>
            <input
              type="text"
              id="host"
              className={`form-input ${errors.host ? 'error' : ''}`}
              value={formData.host}
              onChange={(e) => handleInputChange('host', e.target.value)}
              placeholder={t('databaseConnections.placeholders.host')}
            />
            {errors.host && <span className="error-text">{errors.host}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="port" className="form-label">
              {t('databaseConnections.fields.port')} <span className="required">*</span>
            </label>
            <input
              type="number"
              id="port"
              className={`form-input ${errors.port ? 'error' : ''}`}
              value={formData.port}
              onChange={(e) => handleInputChange('port', parseInt(e.target.value, 10))}
              min="1"
              max="65535"
            />
            {errors.port && <span className="error-text">{errors.port}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="databaseName" className="form-label">
              {t('databaseConnections.fields.database')} <span className="required">*</span>
            </label>
            <input
              type="text"
              id="databaseName"
              className={`form-input ${errors.databaseName ? 'error' : ''}`}
              value={formData.databaseName}
              onChange={(e) => handleInputChange('databaseName', e.target.value)}
              placeholder={t('databaseConnections.placeholders.database')}
            />
            {errors.databaseName && <span className="error-text">{errors.databaseName}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="username" className="form-label">
              {t('databaseConnections.fields.username')} <span className="required">*</span>
            </label>
            <input
              type="text"
              id="username"
              className={`form-input ${errors.username ? 'error' : ''}`}
              value={formData.username}
              onChange={(e) => handleInputChange('username', e.target.value)}
              placeholder={t('databaseConnections.placeholders.username')}
            />
            {errors.username && <span className="error-text">{errors.username}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="password" className="form-label">
              {t('databaseConnections.fields.password')} {!isEditing && <span className="required">*</span>}
            </label>
            <input
              type="password"
              id="password"
              className={`form-input ${errors.password ? 'error' : ''}`}
              value={formData.password}
              onChange={(e) => handleInputChange('password', e.target.value)}
              placeholder={isEditing 
                ? t('databaseConnections.placeholders.passwordEdit') 
                : t('databaseConnections.placeholders.password')
              }
            />
            {errors.password && <span className="error-text">{errors.password}</span>}
            {isEditing && (
              <small className="form-hint">
                {t('databaseConnections.hints.passwordEdit')}
              </small>
            )}
          </div>

          <div className="form-group form-group-full">
            <label htmlFor="connectionParams" className="form-label">
              {t('databaseConnections.fields.connectionParams')}
            </label>
            <input
              type="text"
              id="connectionParams"
              className="form-input"
              value={formData.connectionParams}
              onChange={(e) => handleInputChange('connectionParams', e.target.value)}
              placeholder={t('databaseConnections.placeholders.connectionParams')}
            />
            <small className="form-hint">
              {t('databaseConnections.hints.connectionParams')}
            </small>
          </div>

          <div className="form-group form-group-full">
            <label className="form-checkbox">
              <input
                type="checkbox"
                checked={formData.active}
                onChange={(e) => handleInputChange('active', e.target.checked)}
              />
              <span className="checkbox-label">
                {t('databaseConnections.fields.active')}
              </span>
            </label>
            <small className="form-hint">
              {t('databaseConnections.hints.active')}
            </small>
          </div>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="button button-secondary"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            {t('common.cancel')}
          </button>
          <button
            type="submit"
            className="button button-primary"
            disabled={isSubmitting}
          >
            {isSubmitting 
              ? t('common.saving')
              : (isEditing ? t('common.update') : t('common.create'))
            }
          </button>
        </div>
      </form>
    </div>
  )
}
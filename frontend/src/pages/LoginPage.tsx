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

import React, {useCallback, useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {Link, useNavigate} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'
import type {LoginCredentials} from '../types/frontend'
import '../styles/layouts/AuthLayout.css'
import '../styles/components/Form.css'
import '../styles/components/Button.css'
import '../styles/components/Alert.css'

export const LoginPage: React.FC = () => {
  const {t} = useTranslation()
  const {login, isAuthenticated, isLoading} = useAuth()
  const navigate = useNavigate()
  const [credentials, setCredentials] = useState<LoginCredentials>({
    email: '',
    password: ''
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', {replace: true})
    }
  }, [isAuthenticated, navigate])

  const handleLoginSuccess = useCallback(() => {
    navigate('/dashboard', {replace: true})
  }, [navigate])

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {name, value} = event.target
    setCredentials(prev => ({...prev, [name]: value}))
    setError(null)
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()

    if (!credentials.email || !credentials.password) {
      setError(t('login.validation.required'))
      return
    }

    try {
      await login(credentials)
      handleLoginSuccess()
    } catch (error) {
      setError(error instanceof Error ? error.message : t('login.error.failed'))
    }
  }

  if (isAuthenticated) {
    return null
  }

  return (
    <div className="auth-layout">
      <div className="auth-container">
        <div className="auth-header">
          <h1 className="auth-title">{t('app.title')}</h1>
          <p className="auth-subtitle">{t('app.subtitle')}</p>
        </div>

        <div className="auth-content">
          <form className="form" onSubmit={handleSubmit}>
            <div className="form-header">
              <h1 className="form-title">{t('login.title')}</h1>
            </div>

            {error && (
              <div className="alert alert-error" role="alert">
                {error}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="email" className="form-label">
                {t('login.email.label')}
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={credentials.email}
                onChange={handleInputChange}
                className="form-input"
                required
                autoComplete="email"
                placeholder={t('login.email.placeholder')}
              />
            </div>

            <div className="form-group">
              <label htmlFor="password" className="form-label">
                {t('login.password.label')}
              </label>
              <input
                id="password"
                name="password"
                type="password"
                value={credentials.password}
                onChange={handleInputChange}
                className="form-input"
                required
                autoComplete="current-password"
                placeholder={t('login.password.placeholder')}
              />
            </div>

            <div className="form-actions">
              <button
                type="submit"
                className="button button-primary button-lg button-full"
                disabled={isLoading || !credentials.email || !credentials.password}
              >
                {isLoading ? t('login.submitting') : t('login.submit')}
              </button>
            </div>
          </form>

          <div className="auth-links">
            <p>
              {t('auth.noAccount')}
              <Link to="/register-email" className="auth-link">
                {t('auth.register')}
              </Link>
              {t('auth.registerSuffix')}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

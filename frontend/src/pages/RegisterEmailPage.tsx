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

import React, {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {Link, useNavigate} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'
import {LanguageSelector} from '../components/LanguageSelector'
import {authService} from '../services/authService'
import type {RegisterEmailCredentials} from '../types/frontend'

export const RegisterEmailPage: React.FC = () => {
  const {t, i18n} = useTranslation()
  const {isAuthenticated} = useAuth()
  const navigate = useNavigate()
  const [registrationComplete, setRegistrationComplete] = useState(false)
  const [credentials, setCredentials] = useState<RegisterEmailCredentials>({
    email: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', {replace: true})
    }
  }, [isAuthenticated, navigate])

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {name, value} = event.target
    setCredentials(prev => ({...prev, [name]: value}))
    setError(null)
  }

  const validateForm = (): string | null => {
    if (!credentials.email) {
      return t('registerEmail.validation.required')
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(credentials.email)) {
      return t('registerEmail.validation.invalidEmail')
    }

    return null
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()

    const validationError = validateForm()
    if (validationError) {
      setError(validationError)
      return
    }

    setIsLoading(true)
    try {
      await authService.registerEmail({
        ...credentials,
        language: i18n.language
      })
      // Store email for success message display
      sessionStorage.setItem('registrationEmail', credentials.email)
      setRegistrationComplete(true)
    } catch (error) {
      setError(error instanceof Error ? error.message : t('registerEmail.error.failed'))
    } finally {
      setIsLoading(false)
    }
  }

  if (isAuthenticated) {
    return null
  }

  if (registrationComplete) {
    return (
      <div className="auth-layout">
        <div className="auth-container">
          <div className="auth-header">
            <h1 className="auth-title">{t('app.title')}</h1>
            <p className="auth-subtitle">{t('app.subtitle')}</p>
          </div>

          <div className="auth-content">
            <div className="alert alert-success" role="alert">
              <h2>{t('registerEmail.success.title')}</h2>
              <p>{t('registerEmail.success.message', {email: sessionStorage.getItem('registrationEmail') || ''})}</p>
              <p>{t('registerEmail.success.instruction')}</p>
            </div>

            <div className="form-actions" style={{marginTop: '1.5rem'}}>
              <Link
                to="/login"
                className="button button-primary button-lg button-full"
              >
                {t('common.confirm')}
              </Link>
            </div>
          </div>
        </div>
      </div>
    )
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
              <h1 className="form-title">{t('registerEmail.title')}</h1>
              <p className="form-subtitle">{t('registerEmail.description')}</p>
            </div>

            {error && (
              <div className="alert alert-error" role="alert">
                {error}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="email" className="form-label">
                {t('registerEmail.email.label')}
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
                placeholder={t('registerEmail.email.placeholder')}
              />
            </div>

            <div className="form-actions">
              <button
                type="submit"
                className="button button-primary button-lg button-full"
                disabled={isLoading}
              >
                {isLoading ? t('registerEmail.submitting') : t('registerEmail.submit')}
              </button>
            </div>
          </form>

          <div className="auth-links">
            <p>
              {t('registerEmail.hasAccount')}
              <Link to="/login" className="auth-link">
                {t('registerEmail.loginLink')}
              </Link>
            </p>
          </div>

          <div className="language-selector-container">
            <LanguageSelector/>
          </div>
        </div>
      </div>
    </div>
  )
}

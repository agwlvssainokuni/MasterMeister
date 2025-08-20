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

import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {Link, useNavigate, useSearchParams} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'
import {authService} from '../services/authService'
import type {EmailConfirmationResult} from '../types/api'
import '../styles/layouts/AuthLayout.css'
import '../styles/components/Alert.css'
import '../styles/components/Loading.css'

export const EmailConfirmationPage = () => {
  const {t} = useTranslation()
  const {isAuthenticated} = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [result, setResult] = useState<EmailConfirmationResult | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', {replace: true})
      return
    }

    const token = searchParams.get('token')

    if (!token) {
      setError(t('emailConfirmation.error.missingToken'))
      setIsLoading(false)
      return
    }

    const confirmEmail = async () => {
      try {
        const confirmationResult = await authService.confirmEmail(token)
        setResult(confirmationResult)
      } catch (error) {
        setError(error instanceof Error ? error.message : t('emailConfirmation.error.failed'))
      } finally {
        setIsLoading(false)
      }
    }

    confirmEmail()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated, navigate])

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
          {isLoading ? (
            <div className="loading-container">
              <div className="spinner"></div>
              <p>{t('emailConfirmation.confirming')}</p>
            </div>
          ) : error ? (
            <div className="alert alert-error" role="alert">
              <h2>{t('emailConfirmation.error.title')}</h2>
              <p>{error}</p>
            </div>
          ) : result ? (
            <div className={`alert ${result.status === 'SUCCESS' ? 'alert-success' : 'alert-error'}`} role="alert">
              <h2>
                {result.status === 'SUCCESS'
                  ? t('emailConfirmation.success.title')
                  : t('emailConfirmation.error.title')
                }
              </h2>
              <p>{result.message}</p>
            </div>
          ) : null}

          <div className="form-actions" style={{marginTop: '1.5rem'}}>
            <Link
              to="/login"
              className="button button-primary button-lg button-full"
            >
              {t('emailConfirmation.loginButton')}
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

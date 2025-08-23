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
import {Link, useNavigate} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'
import {RegisterEmailForm} from '../components/RegisterEmailForm.tsx'
import '../styles/layouts/AuthLayout.css'
import '../styles/components/Alert.css'

export const RegisterEmailPage = () => {
  const {t} = useTranslation()
  const {isAuthenticated} = useAuth()
  const navigate = useNavigate()
  const [registrationComplete, setRegistrationComplete] = useState(false)

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', {replace: true})
    }
  }, [isAuthenticated, navigate])

  const handleRegisterSuccess = () => {
    setRegistrationComplete(true)
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
              <p>{t('registerEmail.success.message', { email: sessionStorage.getItem('registrationEmail') || '' })}</p>
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
          <RegisterEmailForm onRegisterSuccess={handleRegisterSuccess}/>

          <div className="auth-links">
            <p>
              {t('registerEmail.hasAccount')}
              <Link to="/login" className="auth-link">
                {t('registerEmail.loginLink')}
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

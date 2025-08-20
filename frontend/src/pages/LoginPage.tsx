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

import {useCallback, useEffect} from 'react'
import {useTranslation} from 'react-i18next'
import {useNavigate} from 'react-router-dom'
import {useAuth} from '../contexts/AuthContext'
import {LoginForm} from '../components/LoginForm'
import '../styles/layouts/AuthLayout.css'

export const LoginPage = () => {
  const {t} = useTranslation()
  const {isAuthenticated} = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', {replace: true})
    }
  }, [isAuthenticated, navigate])

  const handleLoginSuccess = useCallback(() => {
    navigate('/dashboard', {replace: true})
  }, [navigate])

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
          <LoginForm onLoginSuccess={handleLoginSuccess}/>
        </div>
      </div>
    </div>
  )
}

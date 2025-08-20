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

import {useState} from 'react'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../contexts/AuthContext'
import type {LoginCredentials} from '../types/frontend'
import '../styles/components/Form.css'
import '../styles/components/Button.css'
import '../styles/components/Alert.css'

interface LoginFormProps {
  onLoginSuccess?: () => void
}

export const LoginForm = ({onLoginSuccess}: LoginFormProps) => {
  const {t} = useTranslation()
  const {login, isLoading} = useAuth()
  const [credentials, setCredentials] = useState<LoginCredentials>({
    username: '',
    password: ''
  })
  const [error, setError] = useState<string | null>(null)

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {name, value} = event.target
    setCredentials(prev => ({...prev, [name]: value}))
    setError(null)
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()

    if (!credentials.username || !credentials.password) {
      setError(t('login.validation.required'))
      return
    }

    try {
      await login(credentials)
      onLoginSuccess?.()
    } catch (error) {
      setError(error instanceof Error ? error.message : t('login.error.failed'))
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <div className="form-header">
        <h1 className="form-title">{t('login.title')}</h1>
      </div>

      {error && (
        <div className="alert alert--danger" role="alert">
          {error}
        </div>
      )}

      <div className="form-group">
        <label htmlFor="username" className="form-label">
          {t('login.username.label')}
        </label>
        <input
          id="username"
          name="username"
          type="text"
          value={credentials.username}
          onChange={handleInputChange}
          className="form-input"
          required
          autoComplete="username"
          placeholder={t('login.username.placeholder')}
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
          disabled={isLoading || !credentials.username || !credentials.password}
        >
          {isLoading ? t('login.submitting') : t('login.submit')}
        </button>
      </div>
    </form>
  )
}

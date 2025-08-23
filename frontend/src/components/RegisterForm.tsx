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
import {authService} from '../services/authService'
import type {RegistrationCredentials} from '../types/frontend'
import '../styles/components/Form.css'
import '../styles/components/Button.css'
import '../styles/components/Alert.css'

interface RegisterFormProps {
  onRegisterSuccess?: () => void
}

export const RegisterForm = ({onRegisterSuccess}: RegisterFormProps) => {
  const {t} = useTranslation()
  const [credentials, setCredentials] = useState<RegistrationCredentials>({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {name, value} = event.target
    setCredentials(prev => ({...prev, [name]: value}))
    setError(null)
  }

  const validateForm = (): string | null => {
    if (!credentials.username || !credentials.email || !credentials.password) {
      return t('register.validation.required')
    }

    if (credentials.password !== credentials.confirmPassword) {
      return t('register.validation.passwordMismatch')
    }

    if (credentials.password.length < 8) {
      return t('register.validation.passwordTooShort')
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(credentials.email)) {
      return t('register.validation.invalidEmail')
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
      await authService.register(credentials)
      onRegisterSuccess?.()
    } catch (error) {
      setError(error instanceof Error ? error.message : t('register.error.failed'))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <div className="form-header">
        <h1 className="form-title">{t('register.title')}</h1>
      </div>

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      <div className="form-group">
        <label htmlFor="username" className="form-label">
          {t('register.username.label')}
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
          placeholder={t('register.username.placeholder')}
        />
      </div>

      <div className="form-group">
        <label htmlFor="email" className="form-label">
          {t('register.email.label')}
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
          placeholder={t('register.email.placeholder')}
        />
      </div>

      <div className="form-group">
        <label htmlFor="password" className="form-label">
          {t('register.password.label')}
        </label>
        <input
          id="password"
          name="password"
          type="password"
          value={credentials.password}
          onChange={handleInputChange}
          className="form-input"
          required
          autoComplete="new-password"
          placeholder={t('register.password.placeholder')}
        />
      </div>

      <div className="form-group">
        <label htmlFor="confirmPassword" className="form-label">
          {t('register.confirmPassword.label')}
        </label>
        <input
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          value={credentials.confirmPassword}
          onChange={handleInputChange}
          className="form-input"
          required
          autoComplete="new-password"
          placeholder={t('register.confirmPassword.placeholder')}
        />
      </div>

      <div className="form-actions">
        <button
          type="submit"
          className="button button-primary button-lg button-full"
          disabled={isLoading}
        >
          {isLoading ? t('register.submitting') : t('register.submit')}
        </button>
      </div>
    </form>
  )
}

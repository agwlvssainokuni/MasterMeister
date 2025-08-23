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
import type {RegisterEmailCredentials} from '../types/frontend'
import '../styles/components/Form.css'
import '../styles/components/Button.css'
import '../styles/components/Alert.css'

interface RegisterEmailFormProps {
  onRegisterSuccess?: () => void
}

export const RegisterEmailForm = ({onRegisterSuccess}: RegisterEmailFormProps) => {
  const {t} = useTranslation()
  const [credentials, setCredentials] = useState<RegisterEmailCredentials>({
    email: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

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
      await authService.registerEmail(credentials)
      // Store email for success message display
      sessionStorage.setItem('registrationEmail', credentials.email)
      onRegisterSuccess?.()
    } catch (error) {
      setError(error instanceof Error ? error.message : t('registerEmail.error.failed'))
    } finally {
      setIsLoading(false)
    }
  }

  return (
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
  )
}

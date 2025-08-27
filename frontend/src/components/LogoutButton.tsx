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

import React, {useState} from 'react'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../contexts/AuthContext'
import '../styles/components/Button.css'
import '../styles/components/Alert.css'

interface LogoutButtonProps {
  variant?: 'primary' | 'secondary'
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export const LogoutButton: React.FC<LogoutButtonProps> = (
  {
    variant = 'secondary',
    size = 'md',
    className = ''
  }
) => {
  const {t} = useTranslation()
  const {logout, isLoading} = useAuth()
  const [error, setError] = useState<string | null>(null)

  const handleLogout = async () => {
    try {
      setError(null)
      await logout()
    } catch (error) {
      setError(error instanceof Error ? error.message : t('logout.error.failed'))
    }
  }

  const buttonClasses = [
    'button',
    `button-${variant}`,
    size !== 'md' ? `button-${size}` : '',
    className
  ].filter(Boolean).join(' ')

  return (
    <div className="logout-button-container">
      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}
      <button
        type="button"
        onClick={handleLogout}
        className={buttonClasses}
        disabled={isLoading}
      >
        {isLoading ? t('logout.submitting') : t('logout.submit')}
      </button>
    </div>
  )
}

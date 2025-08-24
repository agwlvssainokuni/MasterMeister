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

import React from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { LogoutButton } from '../components/LogoutButton'

export const DashboardPage: React.FC = () => {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>{t('dashboard.title')}</h1>
        <div className="dashboard-user-info">
          <span className="user-email">{user?.email}</span>
          <LogoutButton />
        </div>
      </header>

      <main className="dashboard-main">
        <p className="dashboard-welcome">{t('dashboard.welcome')}</p>
        
        <div className="dashboard-cards">
          <div className="dashboard-card">
            <div className="card-icon">📊</div>
            <h3>{t('dashboard.dataAccess.title')}</h3>
            <p>{t('dashboard.dataAccess.description')}</p>
            <Link to="/data" className="button button-primary">
              {t('dashboard.dataAccess.button')}
            </Link>
          </div>

          {user?.role === 'ADMIN' && (
            <div className="dashboard-card">
              <div className="card-icon">⚙️</div>
              <h3>{t('dashboard.adminPanel.title')}</h3>
              <p>{t('dashboard.adminPanel.description')}</p>
              <Link to="/admin" className="button button-secondary">
                {t('dashboard.adminPanel.button')}
              </Link>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
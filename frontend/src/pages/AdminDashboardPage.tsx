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
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { AdminLayout } from '../components/layouts/AdminLayout'

export const AdminDashboardPage: React.FC = () => {
  const { t } = useTranslation()

  return (
    <AdminLayout title={t('admin.dashboard.title')}>
      <div className="admin-dashboard">
        <div className="dashboard-welcome">
          <h2>{t('admin.dashboard.welcome')}</h2>
          <p>{t('admin.dashboard.description')}</p>
        </div>

        <div className="admin-features-grid">
          <div className="feature-card">
            <div className="feature-icon">👥</div>
            <h3>{t('admin.features.userManagement.title')}</h3>
            <p>{t('admin.features.userManagement.description')}</p>
            <div className="feature-actions">
              <Link to="/admin/users" className="button button-primary">
                {t('admin.features.userManagement.action')}
              </Link>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">🔗</div>
            <h3>{t('admin.features.databaseConnections.title')}</h3>
            <p>{t('admin.features.databaseConnections.description')}</p>
            <div className="feature-actions">
              <Link to="/admin/database-connections" className="button button-primary">
                {t('admin.features.databaseConnections.action')}
              </Link>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">📋</div>
            <h3>{t('admin.features.schemaManagement.title')}</h3>
            <p>{t('admin.features.schemaManagement.description')}</p>
            <div className="feature-actions">
              <Link to="/admin/schema" className="button button-primary">
                {t('admin.features.schemaManagement.action')}
              </Link>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">🔐</div>
            <h3>{t('admin.features.permissions.title')}</h3>
            <p>{t('admin.features.permissions.description')}</p>
            <div className="feature-actions">
              <Link to="/admin/permissions" className="button button-primary">
                {t('admin.features.permissions.action')}
              </Link>
            </div>
          </div>
        </div>
      </div>
    </AdminLayout>
  )
}
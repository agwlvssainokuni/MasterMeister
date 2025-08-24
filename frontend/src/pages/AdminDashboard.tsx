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
import {Link} from 'react-router-dom'
import {AdminLayout} from '../components/layouts/AdminLayout'
import {PendingUsersList} from '../components/PendingUsersList'
import '../styles/components/Tabs.css'
import '../styles/components/Card.css'
import '../styles/pages/AdminDashboard.css'

export const AdminDashboard = () => {
  const {t} = useTranslation()
  const [activeTab, setActiveTab] = useState<'users' | 'database' | 'permissions'>('users')

  return (
    <AdminLayout title={t('admin.dashboard.title')}>
      <div className="tabs">
        <ul className="tabs-list">
          <li className={`tab-item ${activeTab === 'users' ? 'active' : ''}`}>
            <button
              type="button"
              className="tab-button"
              onClick={() => setActiveTab('users')}
            >
              {t('admin.nav.users')}
            </button>
          </li>
          <li className={`tab-item ${activeTab === 'database' ? 'active' : ''}`}>
            <button
              type="button"
              className="tab-button"
              onClick={() => setActiveTab('database')}
            >
              {t('admin.nav.database')}
            </button>
          </li>
          <li className={`tab-item ${activeTab === 'permissions' ? 'active' : ''}`}>
            <button
              type="button"
              className="tab-button"
              onClick={() => setActiveTab('permissions')}
            >
              {t('admin.nav.permissions')}
            </button>
          </li>
        </ul>
      </div>

      <main className="admin-main">
        <div className={`tab-panel ${activeTab === 'users' ? 'active' : ''}`}>
          <div className="admin-section-header">
            <h2 className="admin-section-title">{t('admin.users.title')}</h2>
            <p className="admin-section-description">{t('admin.users.description')}</p>
          </div>
          <PendingUsersList/>
        </div>

        <div className={`tab-panel ${activeTab === 'database' ? 'active' : ''}`}>
          <div className="admin-section-header">
            <h2 className="admin-section-title">{t('admin.database.title')}</h2>
            <p className="admin-section-description">{t('admin.database.description')}</p>
          </div>
          <div className="admin-features-grid">
            <div className="feature-card">
              <div className="feature-icon">🔗</div>
              <h3>Database Connections</h3>
              <p>Manage database connections and configure access to external databases</p>
              <Link to="/admin/database-connections" className="button button-primary">
                Manage Connections
              </Link>
            </div>
            <div className="feature-card">
              <div className="feature-icon">📋</div>
              <h3>Schema Management</h3>
              <p>Read and update database schema metadata for connected databases</p>
              <Link to="/admin/schema" className="button button-primary">
                Manage Schema
              </Link>
            </div>
          </div>
        </div>

        <div className={`tab-panel ${activeTab === 'permissions' ? 'active' : ''}`}>
          <div className="admin-section-header">
            <h2 className="admin-section-title">{t('admin.permissions.title')}</h2>
            <p className="admin-section-description">{t('admin.permissions.description')}</p>
          </div>
          <div className="admin-features-grid single-column">
            <div className="feature-card">
              <div className="feature-icon">🔐</div>
              <h3>Permission Management</h3>
              <p>Export and import user permissions and access control settings via YAML configuration</p>
              <Link to="/admin/permissions" className="button button-primary">
                Manage Permissions
              </Link>
            </div>
          </div>
        </div>
      </main>
    </AdminLayout>
  )
}

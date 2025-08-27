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
import {useTranslation} from 'react-i18next'
import {AdminLayout} from './layouts/AdminLayout'
import {FeatureCard} from '../components/FeatureCard'

export const AdminDashboardPage: React.FC = () => {
  const {t} = useTranslation()

  return (
    <AdminLayout
      title={t('admin.dashboard.title')}
      description={t('admin.dashboard.description')}
      className="admin-dashboard"
    >
      <div className="dashboard-welcome">
        <h2>{t('admin.dashboard.welcome')}</h2>
      </div>

      <div className="card-grid card-grid-4">
        <FeatureCard
          title={t('admin.features.userManagement.title')}
          description={t('admin.features.userManagement.description')}
          actionText={t('admin.features.userManagement.action')}
          actionPath="/admin/users"
        />
        <FeatureCard
          title={t('admin.features.databases.title')}
          description={t('admin.features.databases.description')}
          actionText={t('admin.features.databases.action')}
          actionPath="/admin/databases"
        />
        <FeatureCard
          title={t('admin.features.schemaManagement.title')}
          description={t('admin.features.schemaManagement.description')}
          actionText={t('admin.features.schemaManagement.action')}
          actionPath="/admin/schema"
        />
        <FeatureCard
          title={t('admin.features.permissions.title')}
          description={t('admin.features.permissions.description')}
          actionText={t('admin.features.permissions.action')}
          actionPath="/admin/permissions"
        />
      </div>
    </AdminLayout>
  )
}

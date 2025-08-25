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
import {useAuth} from '../contexts/AuthContext'
import {UserLayout} from '../components/layouts/UserLayout'
import {PageWrapper} from '../components/PageWrapper'
import {FeatureCard} from '../components/FeatureCard'

export const DashboardPage: React.FC = () => {
  const {t} = useTranslation()
  const {user} = useAuth()

  return (
    <UserLayout title={t('dashboard.title')}>
      <PageWrapper className="user-dashboard">
        <div className="dashboard-welcome">
          <p>{t('dashboard.welcome')}</p>
        </div>

        <div className="card-grid card-grid-2">
          <FeatureCard
            title={t('dashboard.dataAccess.title')}
            description={t('dashboard.dataAccess.description')}
            actionText={t('dashboard.dataAccess.button')}
            actionPath="/data"
          />

          {user?.role === 'ADMIN' && (
            <FeatureCard
              title={t('dashboard.adminPanel.title')}
              description={t('dashboard.adminPanel.description')}
              actionText={t('dashboard.adminPanel.button')}
              actionPath="/admin"
            />
          )}
        </div>
      </PageWrapper>
    </UserLayout>
  )
}

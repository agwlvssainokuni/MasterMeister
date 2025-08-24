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
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import { LogoutButton } from '../LogoutButton'
import '../../styles/layouts/UserLayout.css'

interface UserLayoutProps {
  children: React.ReactNode
  title?: string
}

export const UserLayout: React.FC<UserLayoutProps> = ({ children, title }) => {
  const { t } = useTranslation()
  const { user } = useAuth()
  const location = useLocation()

  const navigationItems = [
    {
      path: '/dashboard',
      label: t('navigation.dashboard'),
      icon: '🏠'
    },
    {
      path: '/data',
      label: t('navigation.dataAccess'),
      icon: '📊'
    }
  ]

  const isActivePath = (path: string) => {
    return location.pathname === path
  }

  return (
    <div className="user-layout">
      <header className="layout-header">
        <div className="header-content">
          <div className="header-left">
            <Link to="/dashboard" className="logo">
              <h1 className="app-title">{t('app.title')}</h1>
            </Link>
          </div>
          
          <nav className="main-navigation">
            {navigationItems.map(item => (
              <Link
                key={item.path}
                to={item.path}
                className={`nav-link ${isActivePath(item.path) ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                <span className="nav-label">{item.label}</span>
              </Link>
            ))}
          </nav>

          <div className="header-right">
            <div className="user-info">
              <span className="user-email">{user?.email}</span>
              <span className="user-role">{user?.role}</span>
            </div>
            <LogoutButton />
          </div>
        </div>
      </header>

      <main className="layout-main">
        <div className="main-content">
          {title && (
            <div className="page-header">
              <h1 className="page-title">{title}</h1>
            </div>
          )}
          {children}
        </div>
      </main>

      <footer className="layout-footer">
        <div className="footer-content">
          <p>{t('app.copyright')}</p>
        </div>
      </footer>
    </div>
  )
}
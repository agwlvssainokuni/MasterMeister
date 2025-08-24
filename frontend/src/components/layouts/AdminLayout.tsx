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
import '../../styles/layouts/AdminLayout.css'

interface AdminLayoutProps {
  children: React.ReactNode
  title?: string
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ children, title }) => {
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
    },
    {
      path: '/admin',
      label: t('navigation.admin'),
      icon: '⚙️'
    }
  ]

  const adminSubItems = [
    {
      path: '/admin',
      label: t('navigation.userManagement'),
      icon: '👥'
    },
    {
      path: '/admin/database-connections',
      label: t('navigation.databaseConnections'),
      icon: '🔗'
    },
    {
      path: '/admin/permissions',
      label: t('navigation.permissions'),
      icon: '🔐'
    }
  ]

  const isActivePath = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/')
  }

  const isAdminSection = location.pathname.startsWith('/admin')

  return (
    <div className="admin-layout">
      <header className="layout-header admin-header">
        <div className="header-content">
          <div className="header-left">
            <Link to="/dashboard" className="logo">
              <h1 className="app-title">{t('app.title')}</h1>
              <span className="admin-badge">ADMIN</span>
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
              <span className="user-role admin-role">{user?.role}</span>
            </div>
            <LogoutButton />
          </div>
        </div>
      </header>

      {isAdminSection && (
        <nav className="admin-subnav">
          <div className="subnav-content">
            {adminSubItems.map(item => (
              <Link
                key={item.path}
                to={item.path}
                className={`subnav-link ${isActivePath(item.path) ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                <span className="nav-label">{item.label}</span>
              </Link>
            ))}
          </div>
        </nav>
      )}

      <main className="layout-main admin-main">
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
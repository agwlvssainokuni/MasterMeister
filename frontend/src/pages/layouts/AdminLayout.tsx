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
import {Link, useLocation} from 'react-router-dom'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../../contexts/AuthContext'
import {LogoutButton} from '../../components/LogoutButton'
import {LanguageSelector} from '../../components/LanguageSelector'
import {adminSubNavigationItems, mainNavigationItems} from '../../config/navigation'

interface AdminLayoutProps {
  title?: string
  description?: string
  className?: string
  children: React.ReactNode
}

export const AdminLayout: React.FC<AdminLayoutProps> = (
  {
    title,
    description,
    className = '',
    children,
  }
) => {
  const {t} = useTranslation()
  const {user} = useAuth()
  const location = useLocation()

  const isActivePath = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/')
  }

  return (
    <div className="admin-layout">
      <header className="admin-header">
        <div className="header-content">
          <div className="header-left">
            <Link to="/dashboard" className="logo">
              <h1 className="app-title">{t('app.title')}</h1>
              <span className="admin-badge">ADMIN</span>
            </Link>
          </div>

          <nav className="main-navigation">
            {mainNavigationItems.map(item => (
              <Link
                key={item.path}
                to={item.path}
                className={`nav-link ${isActivePath(item.path) ? 'active' : ''}`}
              >
                <span className="nav-label">{t(item.labelKey)}</span>
              </Link>
            ))}
          </nav>

          <div className="header-right">
            <div className="user-info">
              <span className="user-email">{user?.email}</span>
              <span className="user-role admin-role">{user?.role}</span>
            </div>
            <LanguageSelector/>
            <LogoutButton/>
          </div>
        </div>
      </header>

      <nav className="admin-subnav">
        <div className="subnav-content">
          {adminSubNavigationItems.map(item => (
            <Link
              key={item.path}
              to={item.path}
              className={`subnav-link ${isActivePath(item.path) ? 'active' : ''}`}
            >
              <span className="nav-label">{t(item.labelKey)}</span>
            </Link>
          ))}
        </div>
      </nav>

      <main className="admin-main">
        <div className={`main-content ${className}`}>
          {(title || description) && (
            <div className="page-header">
              {title && <h1 className="page-title">{title}</h1>}
              {description && <p className="page-description">{description}</p>}
            </div>
          )}
          <div className="page-content">
            {children}
          </div>
        </div>
      </main>

      <footer className="admin-footer">
        <div className="footer-content">
          <p>{t('app.copyright')}</p>
        </div>
      </footer>
    </div>
  )
}

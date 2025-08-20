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

import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import type {PendingUser} from '../types/frontend'
import {adminService} from '../services/adminService'
import '../styles/components/Table.css'
import '../styles/components/Button.css'
import '../styles/components/Loading.css'
import '../styles/components/Alert.css'

export const PendingUsersList = () => {
  const {t, i18n} = useTranslation()
  const [users, setUsers] = useState<PendingUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<number | null>(null)

  const fetchPendingUsers = async () => {
    try {
      setLoading(true)
      setError(null)
      const pendingUsers = await adminService.getPendingUsers()
      setUsers(pendingUsers)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('common.error'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPendingUsers()
  }, [])

  const handleApprove = async (user: PendingUser) => {
    try {
      setActionLoading(user.id)
      await adminService.approveUser(user.id)
      setUsers(prev => prev.filter(u => u.id !== user.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : t('admin.users.error'))
    } finally {
      setActionLoading(null)
    }
  }

  const handleReject = async (user: PendingUser) => {
    try {
      setActionLoading(user.id)
      await adminService.rejectUser(user.id)
      setUsers(prev => prev.filter(u => u.id !== user.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : t('admin.users.error'))
    } finally {
      setActionLoading(null)
    }
  }

  const formatDate = (date: Date) => {
    const locale = i18n.language === 'ja' ? 'ja-JP' : 'en-US'
    return new Intl.DateTimeFormat(locale, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  }

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>{t('common.loading')}</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="alert alert-error">
        <p>{error}</p>
        <button
          type="button"
          className="button button-secondary button-sm"
          onClick={fetchPendingUsers}
        >
          {t('common.retry')}
        </button>
      </div>
    )
  }

  if (users.length === 0) {
    return (
      <div className="admin-empty-state">
        <p className="admin-text-muted">{t('admin.users.noPendingUsers')}</p>
      </div>
    )
  }

  return (
    <div className="admin-users-table">
      <table className="table">
        <thead>
        <tr>
          <th>{t('admin.users.table.username')}</th>
          <th>{t('admin.users.table.email')}</th>
          <th>{t('admin.users.table.registeredAt')}</th>
          <th>{t('admin.users.table.actions')}</th>
        </tr>
        </thead>
        <tbody>
        {users.map(user => (
          <tr key={user.id}>
            <td>{user.username}</td>
            <td>{user.email}</td>
            <td>{formatDate(user.registeredAt)}</td>
            <td>
              <div className="admin-actions">
                <button
                  type="button"
                  className="button button-primary button-sm"
                  onClick={() => handleApprove(user)}
                  disabled={actionLoading === user.id}
                >
                  {actionLoading === user.id ? t('admin.users.confirming') : t('admin.users.approve')}
                </button>
                <button
                  type="button"
                  className="button button-danger button-sm"
                  onClick={() => handleReject(user)}
                  disabled={actionLoading === user.id}
                >
                  {actionLoading === user.id ? t('admin.users.confirming') : t('admin.users.reject')}
                </button>
              </div>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </div>
  )
}

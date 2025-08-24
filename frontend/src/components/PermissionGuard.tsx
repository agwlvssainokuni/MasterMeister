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
import type {AccessibleTable} from '../types/frontend'

interface PermissionGuardProps {
  table: AccessibleTable
  requiredPermission: 'read' | 'write' | 'delete' | 'admin'
  children: React.ReactNode
  fallback?: React.ReactNode
  showFallback?: boolean
}

export const PermissionGuard: React.FC<PermissionGuardProps> = ({
                                                                  table,
                                                                  requiredPermission,
                                                                  children,
                                                                  fallback,
                                                                  showFallback = false
                                                                }) => {
  const {t} = useTranslation()

  const hasPermission = (): boolean => {
    switch (requiredPermission) {
      case 'read':
        return table.canRead
      case 'write':
        return table.canWrite
      case 'delete':
        return table.canDelete
      case 'admin':
        return table.canAdmin
      default:
        return false
    }
  }

  if (!hasPermission()) {
    if (showFallback && fallback) {
      return <>{fallback}</>
    }

    if (showFallback) {
      return (
        <div className="permission-denied">
          <span className="permission-denied-text">
            {t('permissions.insufficientPermissions')}
          </span>
        </div>
      )
    }

    return null
  }

  return <>{children}</>
}

interface ConditionalPermissionProps {
  table: AccessibleTable
  requiredPermission: 'read' | 'write' | 'delete' | 'admin'
  children: (hasPermission: boolean) => React.ReactNode
}

export const ConditionalPermission: React.FC<ConditionalPermissionProps> = ({
                                                                              table,
                                                                              requiredPermission,
                                                                              children
                                                                            }) => {
  const hasPermission = (): boolean => {
    switch (requiredPermission) {
      case 'read':
        return table.canRead
      case 'write':
        return table.canWrite
      case 'delete':
        return table.canDelete
      case 'admin':
        return table.canAdmin
      default:
        return false
    }
  }

  return <>{children(hasPermission())}</>
}

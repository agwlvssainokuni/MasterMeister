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

export interface NavigationItem {
  path: string
  labelKey: string
}

export const mainNavigationItems: NavigationItem[] = [
  {
    path: '/dashboard',
    labelKey: 'navigation.dashboard'
  },
  {
    path: '/data',
    labelKey: 'navigation.dataAccess'
  },
  {
    path: '/admin',
    labelKey: 'navigation.admin'
  }
]

export const adminSubNavigationItems: NavigationItem[] = [
  {
    path: '/admin/users',
    labelKey: 'navigation.userManagement'
  },
  {
    path: '/admin/databases',
    labelKey: 'navigation.databaseConnections'
  },
  {
    path: '/admin/schema',
    labelKey: 'admin.nav.schema'
  },
  {
    path: '/admin/permissions',
    labelKey: 'navigation.permissions'
  }
]
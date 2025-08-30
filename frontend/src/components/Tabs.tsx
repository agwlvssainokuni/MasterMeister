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

export interface TabItem {
  id: string
  label: string
  content: React.ReactNode
  disabled?: boolean
}

interface TabsProps {
  items: TabItem[]
  activeTab: string
  onTabChange: (tabId: string) => void
  className?: string
}

export const Tabs: React.FC<TabsProps> = (
  {
    items,
    activeTab,
    onTabChange,
    className = '',
  }
) => {
  return (
    <div className={`tabs ${className}`}>
      {/* Tab Headers */}
      <ul className="tabs-list">
        {items.map(item => (
          <li
            key={item.id}
            className={`tab-item ${activeTab === item.id ? 'active' : ''}`}
          >
            <button
              className="tab-button"
              onClick={() => !item.disabled && onTabChange(item.id)}
              disabled={item.disabled}
            >
              {item.label}
            </button>
          </li>
        ))}
      </ul>

      {/* Tab Content */}
      <div className="tab-content">
        {items.map(item => (
          <div
            key={item.id}
            className={`tab-panel ${activeTab === item.id ? 'active' : ''}`}
          >
            {item.content}
          </div>
        ))}
      </div>
    </div>
  )
}

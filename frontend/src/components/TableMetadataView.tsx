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
import {FaKey, FaLock} from 'react-icons/fa'
import type {AccessibleColumn} from '../types/frontend'

interface TableMetadataViewProps {
  columns: AccessibleColumn[]
}

export const TableMetadataView: React.FC<TableMetadataViewProps> = (
  {
    columns,
  }
) => {
  const {t} = useTranslation()

  return (
    <div className="metadata-view">
      <div className="table-container">
        <table className="table metadata-table table-striped">
          <thead className="metadata-table-header">
          <tr>
            <th className="column-name-header">{t('metadata.columnName')}</th>
            <th className="data-type-header">{t('metadata.dataType')}</th>
            <th className="nullable-header">{t('metadata.nullable')}</th>
            <th className="default-value-header">{t('metadata.defaultValue')}</th>
            <th className="primary-key-header">{t('metadata.primaryKey')}</th>
            <th className="comment-header">{t('metadata.comment')}</th>
          </tr>
          </thead>
          <tbody className="metadata-table-body">
          {columns.map((column, index) => (
            <tr key={column.columnName} className={index % 2 === 0 ? 'even' : 'odd'}>
              <td className="column-name-cell">
                <div className="column-name-info">
                  <span className="column-name">{column.columnName}</span>
                  {column.primaryKey && (
                    <span className="pk-badge" title={t('metadata.primaryKey')}>
                        <FaKey/>
                      </span>
                  )}
                  {!column.nullable && (
                    <span className="not-null-badge" title={t('metadata.notNull')}>
                        <FaLock/>
                      </span>
                  )}
                </div>
              </td>
              <td className="data-type-cell">
                <code className="data-type">{column.dataType}</code>
              </td>
              <td className="nullable-cell">
                  <span className={`nullable-badge ${column.nullable ? 'nullable' : 'not-nullable'}`}>
                    {column.nullable ? t('common.yes') : t('common.no')}
                  </span>
              </td>
              <td className="default-value-cell">
                {column.defaultValue ? (
                  <code className="default-value">{column.defaultValue}</code>
                ) : (
                  <span className="no-default">—</span>
                )}
              </td>
              <td className="primary-key-cell">
                  <span className={`pk-badge ${column.primaryKey ? 'is-pk' : 'not-pk'}`}>
                    {column.primaryKey ? (
                      <><FaKey/> {t('common.yes')}</>
                    ) : (
                      t('common.no')
                    )}
                  </span>
              </td>
              <td className="comment-cell">
                {column.comment ? (
                  <span className="comment-text">{column.comment}</span>
                ) : (
                  <span className="no-comment">—</span>
                )}
              </td>
            </tr>
          ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

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

import React, {useState} from 'react'
import {useTranslation} from 'react-i18next'
import {FaChartBar, FaDatabase, FaEdit} from 'react-icons/fa'
import {useNotification} from '../contexts/NotificationContext'
import {UserLayout} from './layouts/UserLayout'
import {DatabaseTreeView} from '../components/DatabaseTreeView'
import {DataTableView} from '../components/DataTableView'
import {RecordEditModal} from '../components/RecordEditModal'
import {RecordDeleteModal} from '../components/RecordDeleteModal'
import {ConditionalPermission} from '../components/PermissionGuard'
import {dataAccessService} from '../services/dataAccessService'
import type {AccessibleTable, Database, TableRecord} from '../types/frontend'

export const DataAccessPage: React.FC = () => {
  const {t} = useTranslation()
  const {addNotification} = useNotification()

  const [selectedDatabase, setSelectedDatabase] = useState<Database>()
  const [selectedTable, setSelectedTable] = useState<AccessibleTable>()
  const [tableDetails, setTableDetails] = useState<AccessibleTable>()
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [editMode, setEditMode] = useState<'create' | 'edit'>('create')
  const [selectedRecord, setSelectedRecord] = useState<TableRecord>()
  const [dataTableReloadTrigger, setDataTableReloadTrigger] = useState(0) // For triggering DataTable reload

  const handleDatabaseSelect = (database: Database) => {
    setSelectedDatabase(database)
    // Clear table selection when database changes
    setSelectedTable(undefined)
    setTableDetails(undefined)
  }

  const handleTableSelect = async (table: AccessibleTable) => {
    try {
      setSelectedTable(table)
      const details = await dataAccessService.getTableDetails(
        table.connectionId,
        table.schemaName,
        table.tableName
      )
      setTableDetails(details)
    } catch (err) {
      console.error('Error loading table metadata:', err)
      addNotification({
        type: 'error',
        message: t('dataAccess.metadataLoadError'),
      })
    }
  }

  const handleCreateRecord = () => {
    setEditMode('create')
    setSelectedRecord(undefined)
    setEditModalOpen(true)
  }

  const handleEditRecord = (record: TableRecord) => {
    setEditMode('edit')
    setSelectedRecord(record)
    setEditModalOpen(true)
  }

  const handleDeleteRecord = (record: TableRecord) => {
    setSelectedRecord(record)
    setDeleteModalOpen(true)
  }

  const handleDataReload = () => {
    setDataTableReloadTrigger(prev => prev + 1)
  }

  const handleEditSuccess = (message: string) => {
    addNotification({
      type: 'success',
      message,
    })
    handleDataReload() // Trigger DataTable reload
  }

  const handleEditError = (error: string) => {
    addNotification({
      type: 'error',
      message: error,
    })
  }

  const handleDeleteSuccess = (message: string) => {
    addNotification({
      type: 'success',
      message,
    })
    handleDataReload() // Trigger DataTable reload
  }

  const handleDeleteError = (error: string) => {
    addNotification({
      type: 'error',
      message: error,
    })
  }

  return (
    <UserLayout title={t('dataAccess.title')}>
      <p className="page-description">{t('dataAccess.description')}</p>

      <div className="data-access-layout">
        <aside className="tree-sidebar">
          <DatabaseTreeView
            onDatabaseSelect={handleDatabaseSelect}
            onTableSelect={handleTableSelect}
            selectedDatabase={selectedDatabase}
            selectedTable={selectedTable}
          />
        </aside>

        <main className="data-content">
          {selectedTable && tableDetails ? (
            <DataTableView
              connectionId={selectedTable.connectionId}
              schemaName={selectedTable.schemaName}
              tableName={selectedTable.tableName}
              accessibleTable={selectedTable}
              reloadTrigger={dataTableReloadTrigger}
              onRecordEdit={handleEditRecord}
              onRecordDelete={handleDeleteRecord}
              onRecordCreate={handleCreateRecord}
            />
          ) : selectedDatabase ? (
            <div className="empty-content">
              <div className="empty-icon"><FaDatabase size={48}/></div>
              <h3>{t('dataAccess.databaseSelected')}</h3>
              <p>{selectedDatabase.name} ({selectedDatabase.dbType})</p>
              <p className="empty-description">{t('dataAccess.selectTableFromTree')}</p>
            </div>
          ) : (
            <div className="empty-content">
              <div className="empty-icon"><FaChartBar size={48}/></div>
              <h3>{t('dataAccess.selectDatabase')}</h3>
              <p>{t('dataAccess.selectDatabaseDescription')}</p>
            </div>
          )}
        </main>
      </div>

      {/* Create/Edit Modal */}
      {editModalOpen && selectedTable && tableDetails && (
        <RecordEditModal
          isOpen={editModalOpen}
          mode={editMode}
          connectionId={selectedTable.connectionId}
          schemaName={selectedTable.schemaName}
          tableName={selectedTable.tableName}
          columns={tableDetails.columns}
          existingRecord={editMode === 'edit' ? selectedRecord : undefined}
          onClose={() => setEditModalOpen(false)}
          onSuccess={handleEditSuccess}
          onError={handleEditError}
        />
      )}

      {/* Delete Confirmation Modal */}
      {deleteModalOpen && selectedTable && tableDetails && selectedRecord && (
        <RecordDeleteModal
          isOpen={deleteModalOpen}
          connectionId={selectedTable.connectionId}
          schemaName={selectedTable.schemaName}
          tableName={selectedTable.tableName}
          record={selectedRecord}
          columns={tableDetails.columns}
          onClose={() => setDeleteModalOpen(false)}
          onSuccess={handleDeleteSuccess}
          onError={handleDeleteError}
        />
      )}

      {/* Floating Action Button for Create */}
      {selectedTable && (
        <ConditionalPermission table={selectedTable} requiredPermission="write">
          {(hasWritePermission) => (
            <button
              className="fab create-record-fab"
              disabled={!hasWritePermission}
              onClick={() => hasWritePermission && handleCreateRecord()}
              title={hasWritePermission ? t('dataTable.createRecord') : t('permissions.insufficientPermissions')}
            >
              <FaEdit/>
            </button>
          )}
        </ConditionalPermission>
      )}
    </UserLayout>
  )
}

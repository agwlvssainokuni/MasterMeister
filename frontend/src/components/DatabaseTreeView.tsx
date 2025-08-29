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

import React, {useCallback, useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {FaChevronDown, FaChevronRight, FaDatabase, FaFolder, FaFolderOpen, FaTable} from 'react-icons/fa'
import {useNotification} from '../contexts/NotificationContext'
import {dataAccessService} from '../services/dataAccessService'
import type {AccessibleTable, Database} from '../types/frontend'

interface DatabaseNode {
  type: 'database'
  database: Database
  expanded: boolean
  schemas: SchemaNode[]
}

interface SchemaNode {
  type: 'schema'
  connectionId: number
  schemaName: string
  expanded: boolean
  tables: TableNode[]
}

interface TableNode {
  type: 'table'
  table: AccessibleTable
}

type TreeNode = DatabaseNode | SchemaNode | TableNode

interface DatabaseTreeViewProps {
  onDatabaseSelect?: (database: Database) => void
  onTableSelect?: (table: AccessibleTable) => void
  selectedDatabase?: Database
  selectedTable?: AccessibleTable
}

export const DatabaseTreeView: React.FC<DatabaseTreeViewProps> = (
  {
    onDatabaseSelect,
    onTableSelect,
    selectedDatabase,
    selectedTable,
  }
) => {
  const {t} = useTranslation()
  const {addNotification} = useNotification()

  const [databases, setDatabases] = useState<DatabaseNode[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set())

  const loadDatabases = useCallback(async () => {
    try {
      setLoading(true)
      const dbList = await dataAccessService.getDatabases()
      const databaseNodes: DatabaseNode[] = dbList
        .filter(db => db.active) // Only show active connections
        .map(db => ({
          type: 'database' as const,
          database: db,
          expanded: false,
          schemas: []
        }))
      setDatabases(databaseNodes)
    } catch (error) {
      console.error('Error loading databases:', error)
      addNotification({
        type: 'error',
        message: t('dataAccess.databaseLoadError')
      })
    } finally {
      setLoading(false)
    }
  }, [addNotification, t])

  useEffect(() => {
    loadDatabases()
  }, [loadDatabases])

  const loadSchemas = async (databaseNode: DatabaseNode) => {
    try {
      // For now, we'll use a placeholder schema loading
      // In real implementation, this would call a schema API
      const schemas: SchemaNode[] = [
        {
          type: 'schema' as const,
          connectionId: databaseNode.database.id,
          schemaName: 'public', // Default schema name
          expanded: false,
          tables: []
        }
      ]

      const updatedDatabases = databases.map(db =>
        db.database.id === databaseNode.database.id
          ? {...db, schemas, expanded: true}
          : db
      )
      setDatabases(updatedDatabases)
    } catch (error) {
      console.error('Error loading schemas:', error)
      addNotification({
        type: 'error',
        message: t('dataAccess.schemaLoadError')
      })
    }
  }

  const loadTables = async (schemaNode: SchemaNode) => {
    try {
      const tableList = await dataAccessService.getAccessibleTables(schemaNode.connectionId)
      const tableNodes: TableNode[] = tableList.map(table => ({
        type: 'table' as const,
        table
      }))

      const updatedDatabases = databases.map(db => ({
        ...db,
        schemas: db.schemas.map(schema =>
          schema.connectionId === schemaNode.connectionId &&
          schema.schemaName === schemaNode.schemaName
            ? {...schema, tables: tableNodes, expanded: true}
            : schema
        )
      }))
      setDatabases(updatedDatabases)
    } catch (error) {
      console.error('Error loading tables:', error)
      addNotification({
        type: 'error',
        message: t('dataAccess.tableLoadError')
      })
    }
  }

  const toggleNode = async (nodeId: string, node: TreeNode) => {
    const newExpandedNodes = new Set(expandedNodes)

    if (expandedNodes.has(nodeId)) {
      newExpandedNodes.delete(nodeId)
    } else {
      newExpandedNodes.add(nodeId)

      // Load child nodes when expanding
      if (node.type === 'database') {
        await loadSchemas(node)
      } else if (node.type === 'schema') {
        await loadTables(node)
      }
    }

    setExpandedNodes(newExpandedNodes)
  }

  const handleDatabaseClick = (database: Database) => {
    onDatabaseSelect?.(database)
  }

  const handleTableClick = (table: AccessibleTable) => {
    onTableSelect?.(table)
  }

  const getDatabaseNodeId = (db: Database) => `db-${db.id}`
  const getSchemaNodeId = (connectionId: number, schemaName: string) => `schema-${connectionId}-${schemaName}`

  const renderExpandIcon = (nodeId: string, hasChildren: boolean) => {
    if (!hasChildren) return <div className="tree-spacer"/>

    return expandedNodes.has(nodeId) ? (
      <FaChevronDown className="tree-expand-icon"/>
    ) : (
      <FaChevronRight className="tree-expand-icon"/>
    )
  }

  const renderDatabaseNode = (databaseNode: DatabaseNode) => {
    const nodeId = getDatabaseNodeId(databaseNode.database)
    const isExpanded = expandedNodes.has(nodeId)
    const isSelected = selectedDatabase?.id === databaseNode.database.id

    return (
      <div key={nodeId} className="tree-node-container">
        <div
          className={`tree-node database-node ${isSelected ? 'selected' : ''}`}
          onClick={() => handleDatabaseClick(databaseNode.database)}
        >
          <div
            className="tree-node-toggle"
            onClick={(e) => {
              e.stopPropagation()
              toggleNode(nodeId, databaseNode)
            }}
          >
            {renderExpandIcon(nodeId, true)}
          </div>
          <FaDatabase className="tree-icon database-icon"/>
          <span className="tree-label">{databaseNode.database.name}</span>
          <span className="tree-meta">({databaseNode.database.dbType})</span>
        </div>

        {isExpanded && (
          <div className="tree-children">
            {databaseNode.schemas.map(schema => renderSchemaNode(schema))}
          </div>
        )}
      </div>
    )
  }

  const renderSchemaNode = (schemaNode: SchemaNode) => {
    const nodeId = getSchemaNodeId(schemaNode.connectionId, schemaNode.schemaName)
    const isExpanded = expandedNodes.has(nodeId)

    return (
      <div key={nodeId} className="tree-node-container">
        <div className="tree-node schema-node">
          <div
            className="tree-node-toggle"
            onClick={(e) => {
              e.stopPropagation()
              toggleNode(nodeId, schemaNode)
            }}
          >
            {renderExpandIcon(nodeId, true)}
          </div>
          {isExpanded ? (
            <FaFolderOpen className="tree-icon schema-icon"/>
          ) : (
            <FaFolder className="tree-icon schema-icon"/>
          )}
          <span className="tree-label">{schemaNode.schemaName}</span>
        </div>

        {isExpanded && (
          <div className="tree-children">
            {schemaNode.tables.map(table => renderTableNode(table))}
          </div>
        )}
      </div>
    )
  }

  const renderTableNode = (tableNode: TableNode) => {
    const isSelected = selectedTable?.tableName === tableNode.table.tableName &&
      selectedTable?.schemaName === tableNode.table.schemaName &&
      selectedTable?.connectionId === tableNode.table.connectionId

    return (
      <div key={`table-${tableNode.table.connectionId}-${tableNode.table.schemaName}-${tableNode.table.tableName}`}
           className="tree-node-container">
        <div
          className={`tree-node table-node ${isSelected ? 'selected' : ''}`}
          onClick={() => handleTableClick(tableNode.table)}
        >
          <div className="tree-spacer"/>
          <FaTable className="tree-icon table-icon"/>
          <span className="tree-label">{tableNode.table.tableName}</span>
          <span className="tree-meta">({tableNode.table.tableType})</span>
        </div>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="tree-loading">
        <div className="loading-spinner"/>
        <p>{t('common.loading')}</p>
      </div>
    )
  }

  if (databases.length === 0) {
    return (
      <div className="tree-empty">
        <FaDatabase className="empty-icon"/>
        <p>{t('dataAccess.noDatabases')}</p>
      </div>
    )
  }

  return (
    <div className="database-tree">
      <div className="tree-header">
        <h3>{t('dataAccess.databaseTree')}</h3>
      </div>
      <div className="tree-content">
        {databases.map(database => renderDatabaseNode(database))}
      </div>
    </div>
  )
}

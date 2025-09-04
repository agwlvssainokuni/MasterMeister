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

package cherry.mastermeister.service;

import cherry.mastermeister.entity.SchemaMetadataEntity;
import cherry.mastermeister.entity.TableMetadataEntity;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.exception.DatabaseNotFoundException;
import cherry.mastermeister.exception.TableNotFoundException;
import cherry.mastermeister.model.AccessibleColumn;
import cherry.mastermeister.model.AccessibleTable;
import cherry.mastermeister.repository.SchemaMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DataAccessService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SchemaMetadataRepository schemaMetadataRepository;
    private final PermissionService permissionService;

    public DataAccessService(
            SchemaMetadataRepository schemaMetadataRepository,
            PermissionService permissionService
    ) {
        this.schemaMetadataRepository = schemaMetadataRepository;
        this.permissionService = permissionService;
    }

    /**
     * Get all available tables for a connection with permission information
     * Used for metadata display where all table information should be visible
     */
    public List<AccessibleTable> getAllAvailableTables(
            Long userId, Long connectionId
    ) {
        logger.info("Retrieving all available tables for connection: {}", connectionId);

        // Get schema metadata directly from repository
        SchemaMetadataEntity schemaEntity = schemaMetadataRepository.findByConnectionId(connectionId)
                .orElseThrow(() -> new DatabaseNotFoundException("Schema metadata not found for connection: " + connectionId));

        List<AccessibleTable> accessibleTables = schemaEntity.getTables().stream()
                .map(tableEntity -> convertToAccessibleTable(tableEntity, userId, connectionId, false))
                .collect(Collectors.toList());

        logger.info("Found {} total accessible tables for connection: {}", accessibleTables.size(), connectionId);
        return accessibleTables;
    }

    /**
     * Get accessible table with detailed column permission information
     */
    public AccessibleTable getAccessibleTableWithColumns(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        logger.debug("Getting accessible table with columns for {}.{} on connection {}",
                schemaName, tableName, connectionId);

        // Get table entity from repository
        SchemaMetadataEntity schemaEntity = schemaMetadataRepository.findByConnectionId(connectionId)
                .orElseThrow(() -> new DatabaseNotFoundException("Schema metadata not found for connection: " + connectionId));

        TableMetadataEntity tableEntity = schemaEntity.getTables().stream()
                .filter(table -> schemaName.equals(table.getSchema()) && tableName.equals(table.getTableName()))
                .findFirst()
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + schemaName + "." + tableName));

        // Convert directly to AccessibleTable with columns
        return convertToAccessibleTable(tableEntity, userId, connectionId, true);
    }

    /**
     * Convert TableMetadataEntity to AccessibleTable directly
     */
    private AccessibleTable convertToAccessibleTable(
            TableMetadataEntity tableEntity,
            Long userId, Long connectionId,
            boolean includeColumns
    ) {
        // Get table permissions
        Set<PermissionType> tablePermissions = permissionService.getTablePermissions(
                userId, connectionId,
                tableEntity.getSchema(), tableEntity.getTableName()
        );

        // Convert columns if requested
        List<AccessibleColumn> accessibleColumns = null;
        if (includeColumns) {
            // Get all column names for bulk permission check
            List<String> allColumnNames = tableEntity.getColumns().stream()
                    .map(columnEntity -> columnEntity.getColumnName())
                    .collect(Collectors.toList());

            // Get all column permissions in bulk (optimized)
            Map<String, Set<PermissionType>> bulkColumnPermissions = permissionService.getBulkColumnPermissions(
                    userId, connectionId, tableEntity.getSchema(), tableEntity.getTableName(), allColumnNames
            );

            accessibleColumns = tableEntity.getColumns().stream()
                    .map(columnEntity -> {
                        Set<PermissionType> columnPermissions = bulkColumnPermissions.get(columnEntity.getColumnName());
                        if (columnPermissions == null) {
                            columnPermissions = Set.of();
                        }

                        return new AccessibleColumn(
                                columnEntity.getColumnName(),
                                columnEntity.getDataType(),
                                columnEntity.getColumnSize(),
                                columnEntity.getDecimalDigits(),
                                columnEntity.getNullable(),
                                columnEntity.getDefaultValue(),
                                columnEntity.getComment(),
                                columnEntity.getPrimaryKey(),
                                columnEntity.getAutoIncrement(),
                                columnEntity.getOrdinalPosition(),
                                columnPermissions,
                                columnPermissions.contains(PermissionType.READ),
                                columnPermissions.contains(PermissionType.WRITE),
                                columnPermissions.contains(PermissionType.DELETE),
                                columnPermissions.contains(PermissionType.ADMIN)
                        );
                    })
                    .collect(Collectors.toList());
        }

        return new AccessibleTable(
                connectionId,
                tableEntity.getSchema(),
                tableEntity.getTableName(),
                tableEntity.getTableType(),
                tableEntity.getComment(),
                tablePermissions,
                tablePermissions.contains(PermissionType.READ),
                tablePermissions.contains(PermissionType.WRITE),
                tablePermissions.contains(PermissionType.DELETE),
                tablePermissions.contains(PermissionType.ADMIN),
                accessibleColumns
        );
    }
}

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

import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.util.PermissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DataAccessService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SchemaMetadataStorageService schemaMetadataStorageService;
    private final PermissionUtils permissionUtils;

    public DataAccessService(SchemaMetadataStorageService schemaMetadataStorageService, PermissionUtils permissionUtils) {
        this.schemaMetadataStorageService = schemaMetadataStorageService;
        this.permissionUtils = permissionUtils;
    }

    /**
     * Get accessible tables for current user with READ permission
     */
    public List<TableMetadata> getAccessibleTables(Long connectionId) {
        logger.info("Retrieving accessible tables for connection: {}", connectionId);

        // Get all tables for the connection
        List<TableMetadata> allTables = schemaMetadataStorageService.getTablesForConnection(connectionId);

        // Filter tables based on READ permission
        List<TableMetadata> accessibleTables = permissionUtils.filterByTablePermission(
                allTables, connectionId, PermissionType.READ,
                new PermissionUtils.TableExtractor<TableMetadata>() {
                    @Override
                    public String getSchemaName(TableMetadata table) {
                        return table.schema();
                    }

                    @Override
                    public String getTableName(TableMetadata table) {
                        return table.tableName();
                    }
                }
        );

        logger.info("Found {} accessible tables out of {} total tables",
                accessibleTables.size(), allTables.size());

        return accessibleTables;
    }

    /**
     * Get accessible tables with specific permission type
     */
    public List<TableMetadata> getAccessibleTables(Long connectionId, PermissionType permissionType) {
        logger.info("Retrieving accessible tables for connection: {} with permission: {}",
                connectionId, permissionType);

        // Require at least connection-level permission for the specified type
        permissionUtils.requireConnectionPermission(connectionId, permissionType);

        // Get all tables for the connection
        List<TableMetadata> allTables = schemaMetadataStorageService.getTablesForConnection(connectionId);

        // Filter tables based on specified permission
        List<TableMetadata> accessibleTables = permissionUtils.filterByTablePermission(
                allTables, connectionId, permissionType,
                new PermissionUtils.TableExtractor<TableMetadata>() {
                    @Override
                    public String getSchemaName(TableMetadata table) {
                        return table.schema();
                    }

                    @Override
                    public String getTableName(TableMetadata table) {
                        return table.tableName();
                    }
                }
        );

        logger.info("Found {} accessible tables out of {} total tables for {} permission",
                accessibleTables.size(), allTables.size(), permissionType);

        return accessibleTables;
    }

    /**
     * Check if user has access to specific table
     */
    public boolean hasTableAccess(Long connectionId, String schemaName, String tableName,
                                  PermissionType permissionType) {
        return permissionUtils.hasTablePermission(connectionId, permissionType, schemaName, tableName);
    }

    /**
     * Get table info with permission check
     */
    public TableMetadata getTableInfo(Long connectionId, String schemaName, String tableName) {
        logger.debug("Getting table info for {}.{} on connection {}", schemaName, tableName, connectionId);

        // Check READ permission for the table
        permissionUtils.requireTablePermission(connectionId, PermissionType.READ, schemaName, tableName);

        // Get table information
        return schemaMetadataStorageService.getTableMetadata(connectionId, schemaName, tableName);
    }
}

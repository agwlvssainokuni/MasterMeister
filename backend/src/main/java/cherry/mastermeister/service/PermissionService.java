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

import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.entity.UserPermissionEntity;
import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.enums.UserRole;
import cherry.mastermeister.model.UserPermission;
import cherry.mastermeister.repository.TableMetadataRepository;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;
    private final TableMetadataRepository tableMetadataRepository;

    public PermissionService(
            UserPermissionRepository userPermissionRepository,
            UserRepository userRepository,
            TableMetadataRepository tableMetadataRepository
    ) {
        this.userPermissionRepository = userPermissionRepository;
        this.userRepository = userRepository;
        this.tableMetadataRepository = tableMetadataRepository;
    }

    // ========================================
    // Permission Creation
    // ========================================

    /**
     * Create permission with granted/denied status
     */
    @Transactional
    public UserPermission createPermission(
            Long userId, Long connectionId, PermissionScope scope,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName,
            boolean granted, LocalDateTime expiresAt, String comment
    ) {
        logger.info("{} {} permission on {} scope for user: {}, connection: {}",
                granted ? "Granting" : "Denying", permissionType, scope, userId, connectionId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserPermissionEntity entity = new UserPermissionEntity();
        entity.setUser(user);
        entity.setConnectionId(connectionId);
        entity.setScope(scope);
        entity.setPermissionType(permissionType);
        entity.setSchemaName(schemaName);
        entity.setTableName(tableName);
        entity.setColumnName(columnName);
        entity.setGranted(granted);
        entity.setGrantedBy(user.getEmail());
        entity.setExpiresAt(expiresAt);
        entity.setComment(comment);

        UserPermissionEntity saved = userPermissionRepository.save(entity);

        logger.info("Permission {} successfully with ID: {}", granted ? "granted" : "denied", saved.getId());
        return toModel(saved);
    }

    // ========================================
    // Table-Level Permission Checking
    // ========================================

    /**
     * Get all permission types that specified user has for a specific table
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tablePermissions", key = "#userId + ':' + #connectionId + ':' + #schemaName + ':' + #tableName")
    public Set<PermissionType> getTablePermissions(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return EnumSet.allOf(PermissionType.class);
        }

        Set<PermissionType> permissions = EnumSet.noneOf(PermissionType.class);

        // Check each permission type
        for (PermissionType permissionType : PermissionType.values()) {
            if (hasTablePermission(userId, connectionId, permissionType, schemaName, tableName)) {
                permissions.add(permissionType);
            }
        }

        return permissions;
    }

    /**
     * Check if user has read permission for a table
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "readPermissions", key = "#userId + ':' + #connectionId + ':' + #schemaName + ':' + #tableName")
    public boolean hasReadPermission(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        return hasTablePermission(
                userId, connectionId,
                PermissionType.READ,
                schemaName, tableName
        );
    }

    /**
     * Check if user can delete entire table (all columns must have DELETE permission)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "deletePermissions", key = "#userId + ':' + #connectionId + ':' + #schemaName + ':' + #tableName")
    public boolean hasDeletePermission(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        return hasTablePermission(
                userId, connectionId,
                PermissionType.DELETE,
                schemaName, tableName
        );
    }

    // ========================================
    // Column-Level Permission Checking
    // ========================================

    /**
     * Get column permissions for multiple columns in bulk (optimized)
     */
    @Transactional(readOnly = true)
    public Map<String, Set<PermissionType>> getBulkColumnPermissions(
            Long userId, Long connectionId,
            String schemaName, String tableName, List<String> columnNames
    ) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            Set<PermissionType> allPermissions = EnumSet.allOf(PermissionType.class);
            return columnNames.stream()
                    .collect(Collectors.toMap(
                            columnName -> columnName,
                            columnName -> allPermissions
                    ));
        }

        // Get upper-level permissions once for all columns
        Map<PermissionType, Boolean> upperLevelPermissions = getUpperLevelPermissions(
                userId, connectionId, schemaName, tableName);

        // Get all column-level permissions for this table in one query
        List<UserPermissionEntity> columnPermissions = userPermissionRepository
                .findAllColumnPermissionsForTable(userId, connectionId, schemaName, tableName);

        // Group column permissions by column name and permission type
        Map<String, Map<PermissionType, Boolean>> columnPermissionMap = columnPermissions.stream()
                .collect(Collectors.groupingBy(
                        UserPermissionEntity::getColumnName,
                        Collectors.toMap(
                                UserPermissionEntity::getPermissionType,
                                UserPermissionEntity::getGranted,
                                (existing, replacement) -> replacement  // Handle duplicates
                        )
                ));

        // Calculate final permissions for each column
        return columnNames.stream()
                .collect(Collectors.toMap(
                        columnName -> columnName,
                        columnName -> calculatePermissionsForColumn(
                                columnPermissionMap.get(columnName), upperLevelPermissions)
                ));
    }

    /**
     * Get list of columns that user can read from
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "readableColumns", key = "#userId + ':' + #connectionId + ':' + #schemaName + ':' + #tableName")
    public List<String> getReadableColumns(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return tableMetadataRepository.findColumnNamesByTable(
                    connectionId, schemaName, tableName);
        }

        // Get all table columns from metadata
        List<String> allTableColumns = tableMetadataRepository.findColumnNamesByTable(
                connectionId, schemaName, tableName);

        // Get all column permissions in bulk (optimized)
        Map<String, Set<PermissionType>> bulkColumnPermissions = getBulkColumnPermissions(
                userId, connectionId, schemaName, tableName, allTableColumns);

        // Filter to only readable columns
        return allTableColumns.stream()
                .filter(columnName -> {
                    Set<PermissionType> permissions = bulkColumnPermissions.get(columnName);
                    return permissions != null && permissions.contains(PermissionType.READ);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get list of columns that user can write to
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "writableColumns", key = "#userId + ':' + #connectionId + ':' + #schemaName + ':' + #tableName")
    public List<String> getWritableColumns(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return tableMetadataRepository.findColumnNamesByTable(
                    connectionId, schemaName, tableName);
        }

        // Get all table columns from metadata
        List<String> allTableColumns = tableMetadataRepository.findColumnNamesByTable(
                connectionId, schemaName, tableName);

        // Get all column permissions in bulk (optimized)
        Map<String, Set<PermissionType>> bulkColumnPermissions = getBulkColumnPermissions(
                userId, connectionId, schemaName, tableName, allTableColumns);

        // Filter to only writable columns
        return allTableColumns.stream()
                .filter(columnName -> {
                    Set<PermissionType> permissions = bulkColumnPermissions.get(columnName);
                    return permissions != null && permissions.contains(PermissionType.WRITE);
                })
                .collect(Collectors.toList());
    }

    // ========================================
    // Permission Retrieval
    // ========================================

    /**
     * Get all active permissions for a user and connection
     */
    @Transactional(readOnly = true)
    public List<UserPermission> getUserPermissions(Long userId, Long connectionId) {
        logger.debug("Retrieving permissions for user: {}, connection: {}", userId, connectionId);

        List<UserPermissionEntity> entities = userPermissionRepository
                .findActivePermissionsByUser(userId, connectionId);

        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Check if user has specific permission for a table (considering column-level permissions)
     */
    private boolean hasTablePermission(
            Long userId, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName
    ) {
        // Get direct table permissions as fallback for columns without explicit permissions
        boolean directTablePermission = checkDirectTablePermissions(
                userId, connectionId,
                permissionType,
                schemaName, tableName
        );

        // Get all table columns from metadata
        List<String> allTableColumns = tableMetadataRepository.findColumnNamesByTable(
                connectionId,
                schemaName, tableName
        );
        if (allTableColumns.isEmpty()) {
            logger.warn("No columns found for table {}.{}", schemaName, tableName);
            return false;
        }

        // Check if ALL columns have specified permission
        for (String columnName : allTableColumns) {
            // Check column-level permission first
            Optional<UserPermissionEntity> columnPerm = userPermissionRepository.findActivePermission(
                    userId, connectionId, PermissionScope.COLUMN,
                    permissionType,
                    schemaName, tableName, columnName
            );

            if (columnPerm.isPresent()) {
                // Explicit column-level setting exists, use it
                if (!columnPerm.get().getGranted()) {
                    return false;
                }
            } else {
                // No column-level setting, use direct table permission
                if (!directTablePermission) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check TABLE/SCHEMA/CONNECTION level permissions (excluding COLUMN scope)
     */
    private boolean checkDirectTablePermissions(
            Long userId, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName
    ) {
        // 1. Check table-level permission
        var tablePerm = userPermissionRepository.findActivePermission(
                userId, connectionId, PermissionScope.TABLE,
                permissionType, schemaName, tableName, null
        ).filter(UserPermissionEntity::getGranted);

        if (tablePerm.isPresent()) {
            return true;
        }

        // 2. Check schema-level permission
        var schemaPerm = userPermissionRepository.findActivePermission(
                userId, connectionId, PermissionScope.SCHEMA,
                permissionType, schemaName, null, null
        ).filter(UserPermissionEntity::getGranted);

        if (schemaPerm.isPresent()) {
            return true;
        }

        // 3. Check connection-level permission  
        var connectionPerm = userPermissionRepository.findActivePermission(
                userId, connectionId, PermissionScope.CONNECTION,
                permissionType, null, null, null
        ).filter(UserPermissionEntity::getGranted);

        return connectionPerm.isPresent();
    }

    /**
     * Get all upper-level permissions (TABLE/SCHEMA/CONNECTION) for all permission types
     */
    private Map<PermissionType, Boolean> getUpperLevelPermissions(
            Long userId, Long connectionId, String schemaName, String tableName
    ) {
        Map<PermissionType, Boolean> results = new HashMap<>();

        for (PermissionType permissionType : PermissionType.values()) {
            results.put(permissionType, checkDirectTablePermissions(
                    userId, connectionId, permissionType, schemaName, tableName));
        }

        return results;
    }

    /**
     * Calculate permissions for a single column using upper-level fallback
     */
    private Set<PermissionType> calculatePermissionsForColumn(
            Map<PermissionType, Boolean> columnLevelPermissions,
            Map<PermissionType, Boolean> upperLevelPermissions
    ) {
        Set<PermissionType> permissions = EnumSet.noneOf(PermissionType.class);

        for (PermissionType permissionType : PermissionType.values()) {
            boolean hasPermission;

            // Check column-level permission first
            if (columnLevelPermissions != null && columnLevelPermissions.containsKey(permissionType)) {
                hasPermission = columnLevelPermissions.get(permissionType);
            } else {
                // No column-level setting, use upper-level permission
                hasPermission = upperLevelPermissions.getOrDefault(permissionType, false);
            }

            if (hasPermission) {
                permissions.add(permissionType);
            }
        }

        return permissions;
    }

    // ========================================
    // Context and Utility Methods
    // ========================================

    /**
     * Check if user is admin
     */
    private boolean isUserAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    /**
     * Convert entity to model
     */
    private UserPermission toModel(UserPermissionEntity entity) {
        return new UserPermission(
                entity.getId(),
                entity.getUser().getId(),
                entity.getConnectionId(),
                entity.getScope(),
                entity.getPermissionType(),
                entity.getSchemaName(),
                entity.getTableName(),
                entity.getColumnName(),
                entity.getGranted(),
                entity.getGrantedBy(),
                entity.getGrantedAt(),
                entity.getExpiresAt(),
                entity.getComment()
        );
    }
}

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
import cherry.mastermeister.model.PermissionCheckResult;
import cherry.mastermeister.model.UserPermission;
import cherry.mastermeister.repository.TableMetadataRepository;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
        entity.setGrantedBy(getCurrentUserEmail());
        entity.setExpiresAt(expiresAt);
        entity.setComment(comment);

        UserPermissionEntity saved = userPermissionRepository.save(entity);

        logger.info("Permission {} successfully with ID: {}", granted ? "granted" : "denied", saved.getId());
        return toModel(saved);
    }

    /**
     * Check permission with current authenticated user
     */
    @Transactional(readOnly = true)
    public PermissionCheckResult checkPermission(
            Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return PermissionCheckResult.denied("No authenticated user");
        }

        String userEmail = auth.getName();
        Long userId = userRepository.findByEmail(userEmail)
                .map(UserEntity::getId)
                .orElse(null);

        if (userId == null) {
            return PermissionCheckResult.denied("User not found");
        }

        return checkPermission(
                userId, connectionId,
                permissionType,
                schemaName, tableName, columnName
        );
    }

    /**
     * Check if user has permission for the requested operation
     */
    @Transactional(readOnly = true)
    public PermissionCheckResult checkPermission(
            Long userId, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName
    ) {
        logger.debug("Checking permission for user: {}, connection: {}, type: {}, schema: {}, table: {}, column: {}",
                userId, connectionId, permissionType,
                schemaName, tableName, columnName);

        // Admin users have full access
        if (isUserAdmin(userId)) {
            logger.debug("Administrator access granted for user: {}", userId);
            return PermissionCheckResult.adminAccess();
        }

        // Check hierarchical permissions from most specific to most general
        PermissionCheckResult result = checkHierarchicalPermissions(
                userId, connectionId,
                permissionType,
                schemaName, tableName, columnName
        );

        if (result.granted()) {
            logger.debug("Permission granted for user: {}, reason: {}", userId, result.reason());
        } else {
            logger.debug("Permission denied for user: {}, reason: {}", userId, result.reason());
        }

        return result;
    }

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

    /**
     * Check if current user is admin
     */
    @Transactional(readOnly = true)
    public boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + UserRole.ADMIN.name()));
    }

    /**
     * Check hierarchical permissions from specific to general
     */
    private PermissionCheckResult checkHierarchicalPermissions(
            Long userId, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName
    ) {

        // 1. Check column-level permission (most specific)
        if (columnName != null) {
            var columnPerm = userPermissionRepository.findActivePermission(
                    userId, connectionId, PermissionScope.COLUMN,
                    permissionType,
                    schemaName, tableName, columnName
            );

            if (columnPerm.isPresent()) {
                return columnPerm.map(perm -> new PermissionCheckResult(
                        perm.getGranted(),
                        PermissionScope.COLUMN, permissionType,
                        "Column-level permission", toModel(perm)
                )).get();
            }
        }

        // 2. Check table-level permission
        if (tableName != null) {
            var tablePerm = userPermissionRepository.findActivePermission(
                    userId, connectionId, PermissionScope.TABLE,
                    permissionType,
                    schemaName, tableName, null
            );

            if (tablePerm.isPresent()) {
                return tablePerm.map(perm -> new PermissionCheckResult(
                        perm.getGranted(),
                        PermissionScope.TABLE, permissionType,
                        "Table-level permission", toModel(perm)
                )).get();
            }
        }

        // 3. Check schema-level permission
        if (schemaName != null) {
            var schemaPerm = userPermissionRepository.findActivePermission(
                    userId, connectionId, PermissionScope.SCHEMA,
                    permissionType,
                    schemaName, null, null
            );

            if (schemaPerm.isPresent()) {
                return schemaPerm.map(perm -> new PermissionCheckResult(
                        perm.getGranted(),
                        PermissionScope.SCHEMA, permissionType,
                        "Schema-level permission", toModel(perm)
                )).get();
            }
        }

        // 4. Check connection-level permission (most general)
        var connectionPerm = userPermissionRepository.findActivePermission(
                userId, connectionId, PermissionScope.CONNECTION,
                permissionType,
                null, null, null
        );

        if (connectionPerm.isPresent()) {
            return connectionPerm.map(perm -> new PermissionCheckResult(
                    perm.getGranted(),
                    PermissionScope.CONNECTION, permissionType,
                    "Connection-level permission", toModel(perm)
            )).get();
        }

        // No permission found
        return PermissionCheckResult.denied("No matching permission found");
    }

    /**
     * Get all permission types that current user has for a specific table
     */
    @Transactional(readOnly = true)
    public Set<PermissionType> getTablePermissions(
            Long connectionId,
            String schemaName, String tableName
    ) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return EnumSet.noneOf(PermissionType.class);
        }

        return getTablePermissions(
                currentUserId, connectionId,
                schemaName, tableName
        );
    }

    /**
     * Get all permission types that specified user has for a specific table
     */
    @Transactional(readOnly = true)
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
     * Check if user has specific permission for a table (considering column-level permissions)
     */
    private boolean hasTablePermission(
            Long userId, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName
    ) {
        // 1. Check if any column has granted permission
        if (userPermissionRepository.hasGrantedColumnPermission(
                userId, connectionId, permissionType, schemaName, tableName)) {
            return true;
        }

        // 2. If no column-level granted permission, check upper levels
        return checkDirectTablePermissions(
                userId, connectionId, permissionType, schemaName, tableName
        ).granted();
    }

    /**
     * Check TABLE/SCHEMA/CONNECTION level permissions (excluding COLUMN scope)
     */
    private PermissionCheckResult checkDirectTablePermissions(
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
            return new PermissionCheckResult(
                    true, PermissionScope.TABLE, permissionType,
                    "Table-level permission", toModel(tablePerm.get())
            );
        }

        // 2. Check schema-level permission
        var schemaPerm = userPermissionRepository.findActivePermission(
                userId, connectionId, PermissionScope.SCHEMA,
                permissionType, schemaName, null, null
        ).filter(UserPermissionEntity::getGranted);

        if (schemaPerm.isPresent()) {
            return new PermissionCheckResult(
                    true, PermissionScope.SCHEMA, permissionType,
                    "Schema-level permission", toModel(schemaPerm.get())
            );
        }

        // 3. Check connection-level permission  
        var connectionPerm = userPermissionRepository.findActivePermission(
                userId, connectionId, PermissionScope.CONNECTION,
                permissionType, null, null, null
        ).filter(UserPermissionEntity::getGranted);

        if (connectionPerm.isPresent()) {
            return new PermissionCheckResult(
                    true, PermissionScope.CONNECTION, permissionType,
                    "Connection-level permission", toModel(connectionPerm.get())
            );
        }

        return PermissionCheckResult.denied("No direct table permission found");
    }

    /**
     * Get all permission types that current user has for a specific column
     */
    @Transactional(readOnly = true)
    public Set<PermissionType> getColumnPermissions(
            Long connectionId,
            String schemaName, String tableName, String columnName
    ) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return EnumSet.noneOf(PermissionType.class);
        }
        return getColumnPermissions(
                currentUserId, connectionId,
                schemaName, tableName, columnName
        );
    }

    /**
     * Get all permission types that specified user has for a specific column
     */
    @Transactional(readOnly = true)
    public Set<PermissionType> getColumnPermissions(
            Long userId, Long connectionId,
            String schemaName, String tableName, String columnName
    ) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return EnumSet.allOf(PermissionType.class);
        }

        Set<PermissionType> permissions = EnumSet.noneOf(PermissionType.class);

        // Check each permission type using full hierarchical check
        for (PermissionType permissionType : PermissionType.values()) {
            PermissionCheckResult result = checkHierarchicalPermissions(
                    userId, connectionId,
                    permissionType,
                    schemaName, tableName, columnName
            );
            if (result.granted()) {
                permissions.add(permissionType);
            }
        }

        return permissions;
    }

    /**
     * Get column permissions for multiple columns in bulk (optimized)
     */
    @Transactional(readOnly = true)
    public Map<String, Set<PermissionType>> getBulkColumnPermissions(
            Long connectionId, String schemaName, String tableName, List<String> columnNames
    ) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return columnNames.stream()
                    .collect(Collectors.toMap(
                            columnName -> columnName,
                            columnName -> EnumSet.noneOf(PermissionType.class)
                    ));
        }
        return getBulkColumnPermissions(currentUserId, connectionId, schemaName, tableName, columnNames);
    }

    /**
     * Get column permissions for multiple columns in bulk (optimized)
     */
    @Transactional(readOnly = true)
    public Map<String, Set<PermissionType>> getBulkColumnPermissions(
            Long userId, Long connectionId, String schemaName, String tableName, List<String> columnNames
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
        Map<PermissionType, PermissionCheckResult> upperLevelPermissions = getAllUpperLevelPermissions(
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
     * Get all upper-level permissions (TABLE/SCHEMA/CONNECTION) for all permission types
     */
    private Map<PermissionType, PermissionCheckResult> getAllUpperLevelPermissions(
            Long userId, Long connectionId, String schemaName, String tableName
    ) {
        Map<PermissionType, PermissionCheckResult> results = new HashMap<>();

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
            Map<PermissionType, PermissionCheckResult> upperLevelPermissions
    ) {
        Set<PermissionType> permissions = EnumSet.noneOf(PermissionType.class);

        for (PermissionType permissionType : PermissionType.values()) {
            boolean hasPermission;

            // Check column-level permission first
            if (columnLevelPermissions != null && columnLevelPermissions.containsKey(permissionType)) {
                hasPermission = columnLevelPermissions.get(permissionType);
            } else {
                // No column-level setting, use upper-level permission
                PermissionCheckResult upperResult = upperLevelPermissions.get(permissionType);
                hasPermission = upperResult != null && upperResult.granted();
            }

            if (hasPermission) {
                permissions.add(permissionType);
            }
        }

        return permissions;
    }

    /**
     * Check if current user can delete entire table (all columns must have DELETE permission)
     */
    @Transactional(readOnly = true)
    public boolean canDeleteTable(
            Long connectionId,
            String schemaName, String tableName
    ) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        return canDeleteTable(
                currentUserId, connectionId,
                schemaName, tableName
        );
    }

    /**
     * Check if user can delete entire table (all columns must have DELETE permission)
     */
    @Transactional(readOnly = true)
    public boolean canDeleteTable(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return true;
        }

        // Get direct table permissions as fallback for columns without explicit permissions
        PermissionCheckResult directTableResult = checkDirectTablePermissions(
                userId, connectionId,
                PermissionType.DELETE,
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

        // Check if ALL columns have DELETE permission
        for (String columnName : allTableColumns) {
            // Check column-level permission first
            Optional<UserPermissionEntity> columnPerm = userPermissionRepository
                    .findByUserIdAndConnectionIdAndScopeAndPermissionTypeAndSchemaNameAndTableNameAndColumnName(
                            userId, connectionId, PermissionScope.COLUMN,
                            PermissionType.DELETE,
                            schemaName, tableName, columnName
                    );

            if (columnPerm.isPresent()) {
                // Explicit column-level setting exists, use it
                if (!columnPerm.get().getGranted()) {
                    return false;
                }
            } else {
                // No column-level setting, use direct table permission
                if (!directTableResult.granted()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if current user has READ permission for a table
     */
    @Transactional(readOnly = true)
    public boolean hasReadPermission(Long connectionId, String schemaName, String tableName) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        return hasReadPermission(currentUserId, connectionId, schemaName, tableName);
    }

    /**
     * Check if user has READ permission for a table
     */
    @Transactional(readOnly = true)
    public boolean hasReadPermission(Long userId, Long connectionId, String schemaName, String tableName) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return true;
        }

        // Get direct table permissions as fallback for columns without explicit permissions
        PermissionCheckResult directTableResult = checkDirectTablePermissions(
                userId, connectionId, PermissionType.READ, schemaName, tableName);

        // If table-level permission is granted, we have access
        if (directTableResult.granted()) {
            return true;
        }

        // Check if any column has READ permission
        return userPermissionRepository.hasGrantedColumnPermission(
                userId, connectionId, PermissionType.READ, schemaName, tableName);
    }

    /**
     * Get list of columns that current user can read from
     */
    @Transactional(readOnly = true)
    public List<String> getReadableColumns(Long connectionId, String schemaName, String tableName) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }
        return getReadableColumns(currentUserId, connectionId, schemaName, tableName);
    }

    /**
     * Get list of columns that user can read from
     */
    @Transactional(readOnly = true)
    public List<String> getReadableColumns(Long userId, Long connectionId, String schemaName, String tableName) {
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
     * Get list of columns that current user can write to
     */
    @Transactional(readOnly = true)
    public List<String> getWritableColumns(
            Long connectionId,
            String schemaName, String tableName
    ) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }
        return getWritableColumns(
                currentUserId, connectionId,
                schemaName, tableName
        );
    }

    /**
     * Get list of columns that user can write to
     */
    @Transactional(readOnly = true)
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

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userRepository::findByEmail)
                .map(UserEntity::getId)
                .orElse(null);
    }

    /**
     * Get current user email from security context
     */
    private String getCurrentUserEmail() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .orElse("system");
    }

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

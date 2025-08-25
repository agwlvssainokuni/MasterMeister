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
import cherry.mastermeister.model.PermissionRequest;
import cherry.mastermeister.model.UserPermission;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PermissionAuthService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

    public PermissionAuthService(
            UserPermissionRepository userPermissionRepository,
            UserRepository userRepository
    ) {
        this.userPermissionRepository = userPermissionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check if user has permission for the requested operation
     */
    public PermissionCheckResult checkPermission(PermissionRequest request) {
        logger.debug("Checking permission for user: {}, connection: {}, type: {}, schema: {}, table: {}, column: {}",
                request.userEmail(), request.connectionId(), request.permissionType(),
                request.schemaName(), request.tableName(), request.columnName());

        // Admin users have full access
        if (isUserAdmin(request.userId())) {
            logger.debug("Administrator access granted for user: {}", request.userEmail());
            return PermissionCheckResult.adminAccess();
        }

        // Check hierarchical permissions from most specific to most general
        PermissionCheckResult result = checkHierarchicalPermissions(request);

        if (result.granted()) {
            logger.debug("Permission granted for user: {}, reason: {}", request.userEmail(), result.reason());
        } else {
            logger.debug("Permission denied for user: {}, reason: {}", request.userEmail(), result.reason());
        }

        return result;
    }

    /**
     * Check permission with current authenticated user
     */
    public PermissionCheckResult checkPermission(Long connectionId, PermissionType permissionType,
                                                 String schemaName, String tableName, String columnName) {
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

        PermissionRequest request = new PermissionRequest(userId, userEmail, connectionId,
                permissionType, schemaName, tableName, columnName);

        return checkPermission(request);
    }

    /**
     * Get all active permissions for a user and connection
     */
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
    private PermissionCheckResult checkHierarchicalPermissions(PermissionRequest request) {
        // 1. Check column-level permission (most specific)
        if (request.columnName() != null) {
            Optional<UserPermissionEntity> columnPerm = userPermissionRepository
                    .findActivePermission(request.userId(), request.connectionId(), PermissionScope.COLUMN,
                            request.permissionType(), request.schemaName(), request.tableName(), request.columnName());

            if (columnPerm.isPresent()) {
                return PermissionCheckResult.granted(PermissionScope.COLUMN, request.permissionType(),
                        "Column-level permission", toModel(columnPerm.get()));
            }
        }

        // 2. Check table-level permission
        if (request.tableName() != null) {
            Optional<UserPermissionEntity> tablePerm = userPermissionRepository
                    .findActivePermission(request.userId(), request.connectionId(), PermissionScope.TABLE,
                            request.permissionType(), request.schemaName(), request.tableName(), null);

            if (tablePerm.isPresent()) {
                return PermissionCheckResult.granted(PermissionScope.TABLE, request.permissionType(),
                        "Table-level permission", toModel(tablePerm.get()));
            }
        }

        // 3. Check schema-level permission
        if (request.schemaName() != null) {
            Optional<UserPermissionEntity> schemaPerm = userPermissionRepository
                    .findActivePermission(request.userId(), request.connectionId(), PermissionScope.SCHEMA,
                            request.permissionType(), request.schemaName(), null, null);

            if (schemaPerm.isPresent()) {
                return PermissionCheckResult.granted(PermissionScope.SCHEMA, request.permissionType(),
                        "Schema-level permission", toModel(schemaPerm.get()));
            }
        }

        // 4. Check connection-level permission (most general)
        Optional<UserPermissionEntity> connectionPerm = userPermissionRepository
                .findActivePermission(request.userId(), request.connectionId(), PermissionScope.CONNECTION,
                        request.permissionType(), null, null, null);

        if (connectionPerm.isPresent()) {
            return PermissionCheckResult.granted(PermissionScope.CONNECTION, request.permissionType(),
                    "Connection-level permission", toModel(connectionPerm.get()));
        }

        // No permission found
        return PermissionCheckResult.denied("No matching permission found");
    }

    /**
     * Get all permission types that current user has for a specific table
     */
    public Set<PermissionType> getUserTablePermissions(Long connectionId, String schemaName, String tableName) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return EnumSet.noneOf(PermissionType.class);
        }

        return getUserTablePermissions(currentUserId, connectionId, schemaName, tableName);
    }

    /**
     * Get all permission types that specified user has for a specific table
     */
    public Set<PermissionType> getUserTablePermissions(Long userId, Long connectionId, String schemaName, String tableName) {
        // Check if user is admin
        if (isUserAdmin(userId)) {
            return EnumSet.allOf(PermissionType.class);
        }

        Set<PermissionType> permissions = EnumSet.noneOf(PermissionType.class);

        // Get user email for the request
        String userEmail = userRepository.findById(userId)
                .map(UserEntity::getEmail)
                .orElse("unknown");

        // Check each permission type
        for (PermissionType permissionType : PermissionType.values()) {
            PermissionRequest request = new PermissionRequest(
                    userId, userEmail, connectionId, permissionType, schemaName, tableName, null);

            PermissionCheckResult result = checkHierarchicalPermissions(request);
            if (result.granted()) {
                permissions.add(permissionType);
            }
        }

        return permissions;
    }

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            String userEmail = authentication.getName();
            return userRepository.findByEmail(userEmail).map(UserEntity::getId).orElse(null);
        }
        return null;
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

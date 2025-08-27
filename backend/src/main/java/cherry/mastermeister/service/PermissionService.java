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
import cherry.mastermeister.model.UserPermission;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PermissionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

    public PermissionService(
            UserPermissionRepository userPermissionRepository,
            UserRepository userRepository
    ) {
        this.userPermissionRepository = userPermissionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Grant permission to user
     */
    public UserPermission grantPermission(Long userId, Long connectionId, PermissionScope scope,
                                         PermissionType permissionType, String schemaName, String tableName,
                                         String columnName, LocalDateTime expiresAt, String comment) {
        logger.info("Granting {} permission on {} scope for user: {}, connection: {}",
                permissionType, scope, userId, connectionId);

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
        entity.setGranted(true);
        entity.setGrantedBy(getCurrentUserEmail());
        entity.setExpiresAt(expiresAt);
        entity.setComment(comment);

        UserPermissionEntity saved = userPermissionRepository.save(entity);
        
        logger.info("Permission granted successfully with ID: {}", saved.getId());
        return toModel(saved);
    }

    /**
     * Revoke permission from user
     */
    public boolean revokePermission(Long permissionId) {
        logger.info("Revoking permission with ID: {}", permissionId);

        Optional<UserPermissionEntity> permission = userPermissionRepository.findById(permissionId);
        if (permission.isPresent()) {
            UserPermissionEntity entity = permission.get();
            entity.setGranted(false);
            userPermissionRepository.save(entity);
            
            logger.info("Permission revoked successfully for user: {}, connection: {}",
                    entity.getUser().getId(), entity.getConnectionId());
            return true;
        }

        logger.warn("Permission not found for revocation: {}", permissionId);
        return false;
    }

    /**
     * Create permission with granted/denied status
     */
    public UserPermission createPermission(Long userId, Long connectionId, PermissionScope scope,
                                         PermissionType permissionType, String schemaName, String tableName,
                                         String columnName, boolean granted, LocalDateTime expiresAt, String comment) {
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
     * Revoke all permissions for user and connection
     */
    public int revokeAllPermissions(Long userId, Long connectionId) {
        logger.info("Revoking all permissions for user: {}, connection: {}", userId, connectionId);

        int revokedCount = userPermissionRepository.revokeAllPermissionsByUser(userId, connectionId);
        
        logger.info("Revoked {} permissions for user: {}, connection: {}", revokedCount, userId, connectionId);
        return revokedCount;
    }

    /**
     * Get user permissions with filters
     */
    @Transactional(readOnly = true)
    public List<UserPermission> getUserPermissions(Long userId, Long connectionId) {
        logger.debug("Retrieving permissions for user: {}, connection: {}", userId, connectionId);

        List<UserPermissionEntity> entities = userPermissionRepository
                .findByUserIdAndConnectionIdOrderByScopeAscSchemaNameAscTableNameAscColumnNameAsc(userId, connectionId);

        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Get permissions by scope
     */
    @Transactional(readOnly = true)
    public List<UserPermission> getPermissionsByScope(Long userId, Long connectionId, PermissionScope scope) {
        logger.debug("Retrieving {} permissions for user: {}, connection: {}", scope, userId, connectionId);

        List<UserPermissionEntity> entities = userPermissionRepository
                .findByUserIdAndConnectionIdAndScope(userId, connectionId, scope);

        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Update permission expiration
     */
    public boolean updatePermissionExpiration(Long permissionId, LocalDateTime newExpiration) {
        logger.info("Updating permission expiration for ID: {} to: {}", permissionId, newExpiration);

        Optional<UserPermissionEntity> permission = userPermissionRepository.findById(permissionId);
        if (permission.isPresent()) {
            UserPermissionEntity entity = permission.get();
            entity.setExpiresAt(newExpiration);
            userPermissionRepository.save(entity);
            
            logger.info("Permission expiration updated successfully");
            return true;
        }

        logger.warn("Permission not found for expiration update: {}", permissionId);
        return false;
    }

    /**
     * Count active permissions for user
     */
    @Transactional(readOnly = true)
    public long countActivePermissions(Long userId, Long connectionId) {
        return userPermissionRepository.countActivePermissionsByUser(userId, connectionId);
    }

    /**
     * Process expired permissions
     */
    public int processExpiredPermissions() {
        logger.info("Processing expired permissions");

        LocalDateTime now = LocalDateTime.now();
        int expiredCount = userPermissionRepository.expireOutdatedPermissions(now);
        
        logger.info("Processed {} expired permissions", expiredCount);
        return expiredCount;
    }

    /**
     * Get current user email from security context
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "system";
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

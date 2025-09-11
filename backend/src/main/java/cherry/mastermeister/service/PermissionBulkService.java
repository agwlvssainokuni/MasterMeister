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

import cherry.mastermeister.entity.DatabaseConnectionEntity;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.entity.UserPermissionEntity;
import cherry.mastermeister.enums.BulkPermissionScope;
import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.model.PermissionBulkCommand;
import cherry.mastermeister.model.PermissionBulkResult;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.repository.DatabaseConnectionRepository;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionBulkService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final DatabaseConnectionRepository databaseConnectionRepository;
    private final SchemaMetadataService schemaMetadataService;

    public PermissionBulkService(
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository,
            DatabaseConnectionRepository databaseConnectionRepository,
            SchemaMetadataService schemaMetadataService
    ) {
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.databaseConnectionRepository = databaseConnectionRepository;
        this.schemaMetadataService = schemaMetadataService;
    }

    // ==============================================
    // パブリックAPIメソッド
    // ==============================================

    public PermissionBulkResult grantBulkPermissions(
            Long connectionId,
            PermissionBulkCommand command,
            String currentUserEmail
    ) {
        logger.info("Starting bulk permission grant: connection={}, types={}, scope={}",
                connectionId, command.permissionTypes(), command.scope());

        List<String> errors = new ArrayList<>();

        // 1. Validate connection exists and is active
        if (!isConnectionValid(connectionId, errors)) {
            return new PermissionBulkResult(0, 0, 0, 0, 0, errors);
        }

        // 2. Get target users
        List<UserEntity> targetUsers = getTargetUsers(command.userEmails());
        if (targetUsers.isEmpty()) {
            errors.add("No valid users found for permission grant");
            return new PermissionBulkResult(0, 0, 0, 0, 0, errors);
        }

        // 3. Create permissions based on scope
        PermissionBulkCounts counts;
        if (command.scope() == BulkPermissionScope.CONNECTION) {
            counts = grantConnectionLevelPermissions(targetUsers, connectionId, command.permissionTypes(), command.description(), currentUserEmail, errors);
        } else if (command.scope() == BulkPermissionScope.SCHEMA) {
            counts = grantSchemaLevelPermissions(targetUsers, connectionId, command.schemaNames(), command.permissionTypes(), command.description(), currentUserEmail, errors);
        } else {
            counts = grantTableLevelPermissions(targetUsers, connectionId, command.tableNames(), command.permissionTypes(), command.description(), currentUserEmail, errors);
        }

        if (counts.processedItems() == 0 && !errors.isEmpty()) {
            return new PermissionBulkResult(targetUsers.size(), 0, 0, 0, 0, errors);
        }

        logger.info("Bulk permission grant completed: {} permissions created, {} updated, {} skipped, {} errors",
                counts.createdPermissions(), counts.updatedPermissions(), counts.skippedExisting(), errors.size());

        return new PermissionBulkResult(targetUsers.size(), counts.processedItems(), counts.createdPermissions(),
                counts.updatedPermissions(), counts.skippedExisting(), errors);
    }

    // ==============================================
    // 前処理・バリデーション
    // ==============================================

    private boolean isConnectionValid(Long connectionId, List<String> errors) {
        Optional<DatabaseConnectionEntity> connection = databaseConnectionRepository.findById(connectionId);
        if (connection.isEmpty()) {
            errors.add("Database connection not found: " + connectionId);
            return false;
        }

        if (!connection.get().isActive()) {
            errors.add("Database connection is not active: " + connectionId);
            return false;
        }

        return true;
    }

    private List<UserEntity> getTargetUsers(List<String> userEmails) {
        if (userEmails == null || userEmails.isEmpty()) {
            // Return all approved users if no specific emails provided
            return userRepository.findByStatus(UserStatus.APPROVED);
        }

        return userEmails.stream()
                .map(userRepository::findByEmail)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(user -> user.getStatus() == UserStatus.APPROVED)
                .collect(Collectors.toList());
    }

    private List<String> getExistingSchemas(Long connectionId, List<String> schemaNames, List<String> errors) {
        if (schemaNames == null || schemaNames.isEmpty()) {
            errors.add("Schema names must be specified for SCHEMA scope");
            return new ArrayList<>();
        }

        Optional<SchemaMetadata> schemaMetadataOpt = schemaMetadataService.getSchemaMetadata(connectionId);
        if (schemaMetadataOpt.isEmpty()) {
            errors.add("Schema metadata not found for connection: " + connectionId);
            return new ArrayList<>();
        }

        SchemaMetadata schemaMetadata = schemaMetadataOpt.get();
        List<String> existingSchemas = schemaMetadata.schemas();

        return schemaNames.stream()
                .filter(existingSchemas::contains)
                .collect(Collectors.toList());
    }

    private List<TableMetadata> getExistingTables(Long connectionId, List<String> tableNames, List<String> errors) {
        if (tableNames == null || tableNames.isEmpty()) {
            errors.add("Table names must be specified for TABLE scope");
            return new ArrayList<>();
        }

        Optional<SchemaMetadata> schemaMetadataOpt = schemaMetadataService.getSchemaMetadata(connectionId);
        if (schemaMetadataOpt.isEmpty()) {
            errors.add("Schema metadata not found for connection: " + connectionId);
            return new ArrayList<>();
        }

        SchemaMetadata schemaMetadata = schemaMetadataOpt.get();
        List<TableMetadata> allTables = schemaMetadata.tables();

        return allTables.stream()
                .filter(table -> {
                    String fullTableName = table.schema() + "." + table.tableName();
                    String tableName = table.tableName();

                    // "スキーマ名.テーブル名" または "テーブル名" のどちらでもマッチする
                    return tableNames.contains(fullTableName) || tableNames.contains(tableName);
                })
                .collect(Collectors.toList());
    }

    // ==============================================
    // CONNECTION レベル権限処理
    // ==============================================

    private PermissionBulkCounts grantConnectionLevelPermissions(
            List<UserEntity> targetUsers,
            Long connectionId,
            List<PermissionType> permissionTypes,
            String description,
            String currentUserEmail,
            List<String> errors
    ) {
        int createdPermissions = 0;
        int updatedPermissions = 0;
        int skippedExisting = 0;

        for (UserEntity user : targetUsers) {
            for (PermissionType permissionType : permissionTypes) {
                PermissionProcessResult result = processConnectionPermission(
                        user, connectionId, permissionType, description, currentUserEmail, errors
                );
                createdPermissions += result.createdPermissions();
                updatedPermissions += result.updatedPermissions();
                skippedExisting += result.skippedExisting();
            }
        }

        return new PermissionBulkCounts(1, createdPermissions, updatedPermissions, skippedExisting);
    }

    private PermissionProcessResult processConnectionPermission(
            UserEntity user,
            Long connectionId,
            PermissionType permissionType,
            String description,
            String currentUserEmail,
            List<String> errors
    ) {
        // Check if permission already exists (regardless of granted status or expiration)
        Optional<UserPermissionEntity> existingPermission = userPermissionRepository.findPermissionByScope(
                user.getId(), connectionId, PermissionScope.CONNECTION,
                permissionType, null, null, null
        );

        if (existingPermission.isPresent()) {
            UserPermissionEntity existing = existingPermission.get();
            if (existing.getGranted()) {
                return new PermissionProcessResult(0, 0, 1); // skipped
            } else {
                // granted=falseをtrueに更新
                existing.setGranted(true);
                existing.setComment(description);
                existing.setGrantedBy(currentUserEmail);
                existing.setGrantedAt(LocalDateTime.now());
                userPermissionRepository.save(existing);
                return new PermissionProcessResult(0, 1, 0); // updated
            }
        }

        // Create new connection-level permission
        UserPermissionEntity permission = new UserPermissionEntity();
        permission.setUser(user);
        permission.setConnectionId(connectionId);
        permission.setPermissionType(permissionType);
        permission.setScope(PermissionScope.CONNECTION);
        permission.setSchemaName(null);
        permission.setTableName(null);
        permission.setColumnName(null);
        permission.setGranted(true);
        permission.setComment(description);
        permission.setGrantedBy(currentUserEmail);
        permission.setGrantedAt(LocalDateTime.now());

        try {
            userPermissionRepository.save(permission);
            return new PermissionProcessResult(1, 0, 0); // created
        } catch (DataIntegrityViolationException e) {
            logger.warn("Constraint violation creating {} permission for user {} on connection: {}",
                    permissionType, user.getEmail(), e.getMessage());
            errors.add(String.format("Failed to grant %s permission to %s for connection due to constraint violation: %s",
                    permissionType, user.getEmail(), e.getMessage()));
            return new PermissionProcessResult(0, 0, 0); // error
        }
    }

    // ==============================================
    // SCHEMA レベル権限処理
    // ==============================================

    private PermissionBulkCounts grantSchemaLevelPermissions(
            List<UserEntity> targetUsers,
            Long connectionId,
            List<String> schemaNames,
            List<PermissionType> permissionTypes,
            String description,
            String currentUserEmail,
            List<String> errors
    ) {
        List<String> existingSchemas = getExistingSchemas(connectionId, schemaNames, errors);
        if (existingSchemas.isEmpty()) {
            if (errors.isEmpty()) {
                errors.add("No existing schemas found matching the specified schema names");
            }
            return new PermissionBulkCounts(0, 0, 0, 0);
        }

        int processedSchemas = existingSchemas.size();
        int createdPermissions = 0;
        int updatedPermissions = 0;
        int skippedExisting = 0;

        for (UserEntity user : targetUsers) {
            for (String schemaName : existingSchemas) {
                for (PermissionType permissionType : permissionTypes) {
                    PermissionProcessResult result = processSchemaPermission(
                            user, connectionId, permissionType, schemaName, description, currentUserEmail, errors
                    );
                    createdPermissions += result.createdPermissions();
                    updatedPermissions += result.updatedPermissions();
                    skippedExisting += result.skippedExisting();
                }
            }
        }

        return new PermissionBulkCounts(processedSchemas, createdPermissions, updatedPermissions, skippedExisting);
    }

    private PermissionProcessResult processSchemaPermission(
            UserEntity user,
            Long connectionId,
            PermissionType permissionType,
            String schemaName,
            String description,
            String currentUserEmail,
            List<String> errors
    ) {
        // Check if permission already exists (regardless of granted status or expiration)
        Optional<UserPermissionEntity> existingPermission = userPermissionRepository.findPermissionByScope(
                user.getId(), connectionId, PermissionScope.SCHEMA,
                permissionType, schemaName, null, null
        );

        if (existingPermission.isPresent()) {
            UserPermissionEntity existing = existingPermission.get();
            if (existing.getGranted()) {
                return new PermissionProcessResult(0, 0, 1); // skipped
            } else {
                // granted=falseをtrueに更新
                existing.setGranted(true);
                existing.setComment(description);
                existing.setGrantedBy(currentUserEmail);
                existing.setGrantedAt(LocalDateTime.now());
                userPermissionRepository.save(existing);
                return new PermissionProcessResult(0, 1, 0); // updated
            }
        }

        // Create new schema-level permission
        UserPermissionEntity permission = new UserPermissionEntity();
        permission.setUser(user);
        permission.setConnectionId(connectionId);
        permission.setPermissionType(permissionType);
        permission.setScope(PermissionScope.SCHEMA);
        permission.setSchemaName(schemaName);
        permission.setTableName(null);
        permission.setColumnName(null);
        permission.setGranted(true);
        permission.setComment(description);
        permission.setGrantedBy(currentUserEmail);
        permission.setGrantedAt(LocalDateTime.now());

        try {
            userPermissionRepository.save(permission);
            return new PermissionProcessResult(1, 0, 0); // created
        } catch (DataIntegrityViolationException e) {
            logger.warn("Constraint violation creating {} permission for user {} on schema {}: {}",
                    permissionType, user.getEmail(), schemaName, e.getMessage());
            errors.add(String.format("Failed to grant %s permission to %s for schema %s due to constraint violation: %s",
                    permissionType, user.getEmail(), schemaName, e.getMessage()));
            return new PermissionProcessResult(0, 0, 0); // error
        }
    }

    // ==============================================
    // TABLE レベル権限処理
    // ==============================================

    private PermissionBulkCounts grantTableLevelPermissions(
            List<UserEntity> targetUsers,
            Long connectionId,
            List<String> tableNames,
            List<PermissionType> permissionTypes,
            String description,
            String currentUserEmail,
            List<String> errors
    ) {
        List<TableMetadata> existingTables = getExistingTables(connectionId, tableNames, errors);
        if (existingTables.isEmpty()) {
            if (errors.isEmpty()) {
                errors.add("No existing tables found matching the specified table names");
            }
            return new PermissionBulkCounts(0, 0, 0, 0);
        }

        int processedTables = existingTables.size();
        int createdPermissions = 0;
        int updatedPermissions = 0;
        int skippedExisting = 0;

        for (UserEntity user : targetUsers) {
            for (TableMetadata table : existingTables) {
                for (PermissionType permissionType : permissionTypes) {
                    PermissionProcessResult result = processTablePermission(
                            user, permissionType, table, connectionId, description, currentUserEmail, errors
                    );
                    createdPermissions += result.createdPermissions();
                    updatedPermissions += result.updatedPermissions();
                    skippedExisting += result.skippedExisting();
                }
            }
        }

        return new PermissionBulkCounts(processedTables, createdPermissions, updatedPermissions, skippedExisting);
    }

    private PermissionProcessResult processTablePermission(
            UserEntity user,
            PermissionType permissionType,
            TableMetadata table,
            Long connectionId,
            String description,
            String currentUserEmail,
            List<String> errors
    ) {
        String target = table.schema() + "." + table.tableName();

        // Check if permission already exists (regardless of granted status or expiration)
        Optional<UserPermissionEntity> existingPermission = userPermissionRepository.findPermissionByScope(
                user.getId(), connectionId, PermissionScope.TABLE, permissionType,
                table.schema(), table.tableName(), null
        );

        if (existingPermission.isPresent()) {
            UserPermissionEntity existing = existingPermission.get();
            if (existing.getGranted()) {
                return new PermissionProcessResult(0, 0, 1); // skipped
            } else {
                // granted=falseをtrueに更新
                existing.setGranted(true);
                existing.setComment(description);
                existing.setGrantedBy(currentUserEmail);
                existing.setGrantedAt(LocalDateTime.now());
                userPermissionRepository.save(existing);
                return new PermissionProcessResult(0, 1, 0); // updated
            }
        }

        // Create new table-level permission
        UserPermissionEntity permission = new UserPermissionEntity();
        permission.setUser(user);
        permission.setConnectionId(connectionId);
        permission.setPermissionType(permissionType);
        permission.setScope(PermissionScope.TABLE);
        permission.setSchemaName(table.schema());
        permission.setTableName(table.tableName());
        permission.setColumnName(null);
        permission.setGranted(true);
        permission.setComment(description);
        permission.setGrantedBy(currentUserEmail);
        permission.setGrantedAt(LocalDateTime.now());

        try {
            userPermissionRepository.save(permission);
            return new PermissionProcessResult(1, 0, 0); // created
        } catch (DataIntegrityViolationException e) {
            logger.warn("Constraint violation creating {} permission for user {} on {}: {}",
                    permissionType, user.getEmail(), target, e.getMessage());
            errors.add(String.format("Failed to grant %s permission to %s for %s due to constraint violation: %s",
                    permissionType, user.getEmail(), target, e.getMessage()));
            return new PermissionProcessResult(0, 0, 0); // error
        }
    }

    // ==============================================
    // データ型定義
    // ==============================================

    /**
     * カウント情報を保持するレコード
     */
    private record PermissionBulkCounts(
            int processedItems,
            int createdPermissions,
            int updatedPermissions,
            int skippedExisting
    ) {
    }

    /**
     * 単一権限処理結果を保持するレコード
     */
    private record PermissionProcessResult(
            int createdPermissions,
            int updatedPermissions,
            int skippedExisting
    ) {
    }

}

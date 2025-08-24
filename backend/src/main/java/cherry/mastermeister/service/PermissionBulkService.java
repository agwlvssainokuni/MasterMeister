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

import cherry.mastermeister.model.BulkPermissionRequest;
import cherry.mastermeister.model.BulkPermissionResult;
import cherry.mastermeister.entity.DatabaseConnectionEntity;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.entity.UserPermissionEntity;
import cherry.mastermeister.enums.BulkPermissionScope;
import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.repository.DatabaseConnectionRepository;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final SchemaMetadataStorageService schemaMetadataStorageService;

    public PermissionBulkService(
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository,
            DatabaseConnectionRepository databaseConnectionRepository,
            SchemaMetadataStorageService schemaMetadataStorageService
    ) {
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.databaseConnectionRepository = databaseConnectionRepository;
        this.schemaMetadataStorageService = schemaMetadataStorageService;
    }

    public BulkPermissionResult grantBulkPermissions(Long connectionId, BulkPermissionRequest request) {
        logger.info("Starting bulk permission grant: connection={}, type={}, scope={}", 
                   connectionId, request.permissionType(), request.scope());

        List<String> errors = new ArrayList<>();
        int processedUsers = 0;
        int processedTables = 0;
        int createdPermissions = 0;
        int skippedExisting = 0;

        try {
            // 1. Validate connection exists and is active
            DatabaseConnectionEntity connection = validateConnection(connectionId);
            
            // 2. Get target users
            List<UserEntity> targetUsers = getTargetUsers(request.userEmails());
            if (targetUsers.isEmpty()) {
                errors.add("No valid users found for permission grant");
                return new BulkPermissionResult(0, 0, 0, 0, errors);
            }
            processedUsers = targetUsers.size();

            // 3. Get target tables based on scope
            List<TableMetadata> targetTables = getTargetTables(connectionId, request);
            if (targetTables.isEmpty()) {
                errors.add("No tables found matching the specified scope");
                return new BulkPermissionResult(processedUsers, 0, 0, 0, errors);
            }
            processedTables = targetTables.size();

            // 4. Create permissions
            String currentUserEmail = getCurrentUserEmail();
            
            for (UserEntity user : targetUsers) {
                for (TableMetadata table : targetTables) {
                    try {
                        // Check if permission already exists
                        Optional<UserPermissionEntity> existingPermission = userPermissionRepository.findActivePermission(
                                user.getId(), connectionId, PermissionScope.TABLE, request.permissionType(),
                                table.schema(), table.tableName(), null);
                        boolean exists = existingPermission.isPresent();

                        if (exists) {
                            skippedExisting++;
                            continue;
                        }

                        // Create new permission
                        UserPermissionEntity permission = new UserPermissionEntity();
                        permission.setUser(user);
                        permission.setConnectionId(connectionId);
                        permission.setPermissionType(request.permissionType());
                        permission.setScope(PermissionScope.TABLE);
                        permission.setSchemaName(table.schema());
                        permission.setTableName(table.tableName());
                        permission.setGranted(true);
                        permission.setComment(request.description());
                        permission.setGrantedBy(currentUserEmail);
                        permission.setGrantedAt(LocalDateTime.now());

                        userPermissionRepository.save(permission);
                        createdPermissions++;

                    } catch (Exception e) {
                        logger.error("Failed to create permission for user {} on table {}.{}: {}", 
                                   user.getEmail(), table.schema(), table.tableName(), e.getMessage());
                        errors.add(String.format("Failed to grant permission to %s for table %s.%s: %s", 
                                 user.getEmail(), table.schema(), table.tableName(), e.getMessage()));
                    }
                }
            }

            logger.info("Bulk permission grant completed: {} permissions created, {} skipped, {} errors", 
                       createdPermissions, skippedExisting, errors.size());

        } catch (Exception e) {
            logger.error("Bulk permission grant failed", e);
            errors.add("Bulk operation failed: " + e.getMessage());
        }

        return new BulkPermissionResult(processedUsers, processedTables, createdPermissions, skippedExisting, errors);
    }

    private DatabaseConnectionEntity validateConnection(Long connectionId) {
        Optional<DatabaseConnectionEntity> connection = databaseConnectionRepository.findById(connectionId);
        if (connection.isEmpty()) {
            throw new IllegalArgumentException("Database connection not found: " + connectionId);
        }
        
        DatabaseConnectionEntity conn = connection.get();
        if (!conn.isActive()) {
            throw new IllegalArgumentException("Database connection is not active: " + connectionId);
        }
        
        return conn;
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

    private List<TableMetadata> getTargetTables(Long connectionId, BulkPermissionRequest request) {
        Optional<SchemaMetadata> schemaMetadataOpt = schemaMetadataStorageService.getSchemaMetadata(connectionId);
        if (schemaMetadataOpt.isEmpty()) {
            throw new IllegalArgumentException("Schema metadata not found for connection: " + connectionId);
        }
        
        SchemaMetadata schemaMetadata = schemaMetadataOpt.get();
        List<TableMetadata> allTables = schemaMetadata.tables();
        
        switch (request.scope()) {
            case ALL_TABLES:
                return filterSystemTables(allTables, request.includeSystemTables());
            
            case SCHEMA:
                if (request.schemaNames() == null || request.schemaNames().isEmpty()) {
                    throw new IllegalArgumentException("Schema names must be specified for SCHEMA scope");
                }
                return allTables.stream()
                        .filter(table -> request.schemaNames().contains(table.schema()))
                        .collect(Collectors.toList());
            
            case TABLE_LIST:
                if (request.tableNames() == null || request.tableNames().isEmpty()) {
                    throw new IllegalArgumentException("Table names must be specified for TABLE_LIST scope");
                }
                return allTables.stream()
                        .filter(table -> request.tableNames().contains(table.schema() + "." + table.tableName()))
                        .collect(Collectors.toList());
            
            default:
                throw new IllegalArgumentException("Unknown bulk permission scope: " + request.scope());
        }
    }

    private List<TableMetadata> filterSystemTables(List<TableMetadata> tables, boolean includeSystemTables) {
        if (includeSystemTables) {
            return tables;
        }
        
        // Filter out common system schemas
        return tables.stream()
                .filter(table -> !isSystemSchema(table.schema()))
                .collect(Collectors.toList());
    }

    private boolean isSystemSchema(String schemaName) {
        String schema = schemaName.toLowerCase();
        return schema.equals("information_schema") ||
               schema.equals("performance_schema") ||
               schema.equals("mysql") ||
               schema.equals("sys") ||
               schema.equals("pg_catalog") ||
               schema.equals("pg_toast") ||
               schema.startsWith("pg_temp");
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return "system";
    }
}
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

import cherry.mastermeister.controller.dto.PermissionExportData;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.entity.UserPermissionEntity;
import cherry.mastermeister.enums.DuplicateHandling;
import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.DatabaseConnection;
import cherry.mastermeister.model.PermissionTemplate;
import cherry.mastermeister.model.PermissionTemplateItem;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PermissionYamlService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionManagementService permissionManagementService;
    private final PermissionTemplateService permissionTemplateService;
    private final DatabaseConnectionService databaseConnectionService;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final ObjectMapper yamlMapper;
    
    @PersistenceContext
    private EntityManager entityManager;

    public PermissionYamlService(
            PermissionManagementService permissionManagementService,
            PermissionTemplateService permissionTemplateService,
            DatabaseConnectionService databaseConnectionService,
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository
    ) {
        this.permissionManagementService = permissionManagementService;
        this.permissionTemplateService = permissionTemplateService;
        this.databaseConnectionService = databaseConnectionService;
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;

        // Configure YAML mapper
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Export permissions for a connection to YAML string
     */
    public String exportPermissionsAsYaml(
            Long connectionId,
            String description
    ) {
        logger.info("Exporting permissions for connection: {}", connectionId);

        try {
            PermissionExportData exportData = buildExportData(connectionId, description);
            return yamlMapper.writeValueAsString(exportData);
        } catch (Exception e) {
            logger.error("Failed to export permissions as YAML", e);
            throw new RuntimeException("YAML export failed", e);
        }
    }

    /**
     * Import permissions from YAML string
     */
    @Transactional
    public PermissionImportResult importPermissionsFromYaml(
            String yamlContent,
            Long targetConnectionId,
            ImportOptions options
    ) {
        logger.info("Importing permissions to connection: {}", targetConnectionId);

        // Validate connection exists
        try {
            databaseConnectionService.getConnection(targetConnectionId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid connection ID: " + targetConnectionId, e);
        }

        try {
            PermissionExportData importData = yamlMapper.readValue(yamlContent, PermissionExportData.class);
            return processImport(importData, targetConnectionId, options);
        } catch (Exception e) {
            logger.error("Failed to import permissions from YAML", e);
            throw new RuntimeException("YAML import failed", e);
        }
    }

    /**
     * Build export data structure
     */
    private PermissionExportData buildExportData(
            Long connectionId,
            String description
    ) {
        // Export info
        PermissionExportData.ExportInfo exportInfo = new PermissionExportData.ExportInfo(
                LocalDateTime.now(), getCurrentUserEmail(), description);

        // Connection info
        DatabaseConnection connection = databaseConnectionService.getConnection(connectionId);
        PermissionExportData.ConnectionInfo connectionInfo = new PermissionExportData.ConnectionInfo(
                connectionId, connection.name(), connection.dbType().name(), connection.databaseName());

        // Export user permissions
        List<PermissionExportData.UserPermissionData> users = exportUserPermissions(connectionId);

        // Export templates
        List<PermissionExportData.TemplateData> templates = exportTemplates(connectionId);

        return new PermissionExportData(exportInfo, connectionInfo, users, templates);
    }

    /**
     * Export user permissions
     */
    private List<PermissionExportData.UserPermissionData> exportUserPermissions(
            Long connectionId
    ) {
        // Get all users with permissions for this connection
        List<UserEntity> allUsers = userRepository.findAll();

        return allUsers.stream()
                .map(user -> {
                    // Get ALL permissions (both granted and denied) for export
                    List<UserPermissionEntity> permissionEntities = userPermissionRepository
                            .findByUserIdAndConnectionIdOrderByScopeAscSchemaNameAscTableNameAscColumnNameAsc(
                                    user.getId(), connectionId);
                    if (!permissionEntities.isEmpty()) {
                        List<PermissionExportData.PermissionData> permissionData = permissionEntities.stream()
                                .map(this::toPermissionDataFromEntity)
                                .toList();
                        return new PermissionExportData.UserPermissionData(user.getEmail(), permissionData);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Export permission templates
     */
    private List<PermissionExportData.TemplateData> exportTemplates(
            Long connectionId
    ) {
        List<PermissionTemplate> templates = permissionTemplateService.getAllTemplatesForConnection(connectionId);

        return templates.stream()
                .map(template -> {
                    List<PermissionExportData.PermissionData> items = template.items().stream()
                            .map(this::toPermissionData)
                            .toList();
                    return new PermissionExportData.TemplateData(
                            template.name(), template.description(), template.isActive(), items);
                })
                .toList();
    }

    /**
     * Convert entity to export data
     */
    private PermissionExportData.PermissionData toPermissionDataFromEntity(
            UserPermissionEntity entity
    ) {
        return new PermissionExportData.PermissionData(
                entity.getScope().name(),
                entity.getPermissionType().name(),
                entity.getSchemaName(),
                entity.getTableName(),
                entity.getColumnName(),
                entity.getGranted(),
                entity.getExpiresAt(),
                entity.getComment()
        );
    }

    /**
     * Convert template item to export data
     */
    private PermissionExportData.PermissionData toPermissionData(
            PermissionTemplateItem item
    ) {
        return new PermissionExportData.PermissionData(
                item.scope().name(),
                item.permissionType().name(),
                item.schemaName(),
                item.tableName(),
                item.columnName(),
                item.granted(),
                null, // Templates don't have expiration
                item.comment()
        );
    }

    /**
     * Process import operation
     */
    private PermissionImportResult processImport(
            PermissionExportData importData,
            Long targetConnectionId,
            ImportOptions options
    ) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int importedUsers = 0;
        int importedTemplates = 0;
        int importedPermissions = 0;
        int updatedPermissions = 0;
        int skippedDuplicates = 0;

        // Clear existing permissions before import (if requested)
        if (options.clearExistingPermissions()) {
            logger.info("Clearing all existing permissions for connection: {}", targetConnectionId);
            int deletedCount = userPermissionRepository.deleteByConnectionId(targetConnectionId);
            entityManager.flush(); // Force immediate execution of the delete query
            logger.info("Deleted {} permissions for connection: {}", deletedCount, targetConnectionId);
        }

        // Import user permissions
        if (options.importUsers()) {
            for (PermissionExportData.UserPermissionData userData : importData.users()) {
                UserEntity user = userRepository.findByEmail(userData.userEmail()).orElse(null);
                if (user == null) {
                    warnings.add("User not found: " + userData.userEmail());
                    continue;
                }

                for (PermissionExportData.PermissionData permData : userData.permissions()) {
                    try {
                        ImportPermissionResult result = importUserPermission(user.getId(), targetConnectionId, permData, options);
                        if (result.wasSkipped()) {
                            skippedDuplicates++;
                        } else if (result.wasUpdated()) {
                            updatedPermissions++;
                        } else {
                            importedPermissions++;
                        }
                    } catch (Exception e) {
                        errors.add("Failed to import permission for user " + userData.userEmail() + ": " + e.getMessage());
                    }
                }
                importedUsers++;
            }
        }

        // Import templates
        if (options.importTemplates()) {
            for (PermissionExportData.TemplateData templateData : importData.templates()) {
                try {
                    importTemplate(targetConnectionId, templateData);
                    importedTemplates++;
                } catch (Exception e) {
                    errors.add("Failed to import template " + templateData.name() + ": " + e.getMessage());
                }
            }
        }

        return new PermissionImportResult(importedUsers, importedTemplates, importedPermissions, updatedPermissions, skippedDuplicates, warnings, errors);
    }

    /**
     * Import user permission with duplicate checking
     */
    private ImportPermissionResult importUserPermission(
            Long userId,
            Long connectionId,
            PermissionExportData.PermissionData permData,
            ImportOptions options
    ) {
        PermissionScope scope = PermissionScope.valueOf(permData.scope());
        PermissionType permissionType = PermissionType.valueOf(permData.permissionType());

        // Check for existing permission for duplicate handling
        Optional<UserPermissionEntity> existing = userPermissionRepository
                .findByUserIdAndConnectionIdAndScopeAndPermissionTypeAndSchemaNameAndTableNameAndColumnName(
                        userId, connectionId, scope, permissionType,
                        permData.schemaName(), permData.tableName(), permData.columnName()
                );
        
        if (existing.isPresent()) {
            switch (options.duplicateHandling()) {
                case SKIP -> {
                    return ImportPermissionResult.skipped("Permission already exists");
                }
                case OVERWRITE -> {
                    // Update existing permission
                    UserPermissionEntity existingEntity = existing.get();
                    existingEntity.setGranted(permData.granted());
                    existingEntity.setExpiresAt(permData.expiresAt());
                    existingEntity.setComment(permData.comment());
                    existingEntity.setGrantedBy(getCurrentUserEmail());
                    existingEntity.setGrantedAt(LocalDateTime.now());
                    userPermissionRepository.save(existingEntity);
                    return ImportPermissionResult.updated("Permission updated successfully");
                }
                case ERROR -> {
                    throw new IllegalArgumentException("Duplicate permission found: " + 
                        permData.scope() + ":" + permData.permissionType() + " for user " + userId);
                }
            }
        }

        permissionManagementService.createPermission(
                userId, connectionId, scope, permissionType,
                permData.schemaName(), permData.tableName(), permData.columnName(),
                permData.granted(), permData.expiresAt(), permData.comment()
        );

        return ImportPermissionResult.imported("Permission created successfully");
    }

    /**
     * Import permission template
     */
    private void importTemplate(
            Long connectionId,
            PermissionExportData.TemplateData templateData
    ) {
        List<PermissionTemplateItem> items = templateData.items().stream()
                .map(this::toTemplateItem)
                .toList();

        permissionTemplateService.createTemplate(
                templateData.name(), templateData.description(), connectionId, items);
    }

    /**
     * Convert export data to template item
     */
    private PermissionTemplateItem toTemplateItem(
            PermissionExportData.PermissionData permData
    ) {
        return new PermissionTemplateItem(
                null, // ID will be set by service
                null, // Template ID will be set by service
                PermissionScope.valueOf(permData.scope()),
                PermissionType.valueOf(permData.permissionType()),
                permData.schemaName(),
                permData.tableName(),
                permData.columnName(),
                permData.granted(),
                permData.comment()
        );
    }

    /**
     * Get current user email
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * Import options configuration
     */
    public record ImportOptions(
            boolean importUsers,
            boolean importTemplates,
            boolean clearExistingPermissions,
            DuplicateHandling duplicateHandling
    ) {
        public static ImportOptions defaultOptions() {
            return new ImportOptions(true, true, false, DuplicateHandling.OVERWRITE);
        }
        
        public static ImportOptions errorOnDuplicateOptions() {
            return new ImportOptions(true, true, false, DuplicateHandling.ERROR);
        }
        
        public static ImportOptions skipOnDuplicateOptions() {
            return new ImportOptions(true, true, false, DuplicateHandling.SKIP);
        }
        
        public static ImportOptions overwriteOnDuplicateOptions() {
            return new ImportOptions(true, true, false, DuplicateHandling.OVERWRITE);
        }
    }

    /**
     * Individual permission import result
     */
    private record ImportPermissionResult(
            ResultType type,
            String message
    ) {
        public enum ResultType {
            IMPORTED,
            SKIPPED, 
            UPDATED
        }
        
        public boolean wasSkipped() {
            return type == ResultType.SKIPPED;
        }
        
        public boolean wasUpdated() {
            return type == ResultType.UPDATED;
        }
        
        public boolean wasImported() {
            return type == ResultType.IMPORTED;
        }
        
        public static ImportPermissionResult skipped(String message) {
            return new ImportPermissionResult(ResultType.SKIPPED, message);
        }
        
        public static ImportPermissionResult imported(String message) {
            return new ImportPermissionResult(ResultType.IMPORTED, message);
        }
        
        public static ImportPermissionResult updated(String message) {
            return new ImportPermissionResult(ResultType.UPDATED, message);
        }
    }

    /**
     * Import result summary
     */
    public record PermissionImportResult(
            int importedUsers,
            int importedTemplates,
            int importedPermissions,
            int updatedPermissions,
            int skippedDuplicates,
            List<String> warnings,
            List<String> errors
    ) {
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean isSuccessful() {
            return errors.isEmpty();
        }
    }
}

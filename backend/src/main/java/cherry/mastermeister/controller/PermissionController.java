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

package cherry.mastermeister.controller;

import cherry.mastermeister.controller.dto.ApiResponse;
import cherry.mastermeister.controller.dto.BulkPermissionRequest;
import cherry.mastermeister.controller.dto.BulkPermissionResponse;
import cherry.mastermeister.controller.dto.PermissionExportData;
import cherry.mastermeister.enums.DuplicateHandling;
import cherry.mastermeister.model.BulkPermissionResult;
import cherry.mastermeister.service.PermissionBulkService;
import cherry.mastermeister.service.PermissionYamlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Permission Management", description = "Permission configuration management including YAML import/export and bulk operations")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionYamlService permissionYamlService;
    private final PermissionBulkService permissionBulkService;

    public PermissionController(PermissionYamlService permissionYamlService, PermissionBulkService permissionBulkService) {
        this.permissionYamlService = permissionYamlService;
        this.permissionBulkService = permissionBulkService;
    }

    @PostMapping("/{connectionId}/bulk-grant")
    @Operation(summary = "Bulk grant permissions", description = "Grant permissions to multiple users across multiple tables")
    public ApiResponse<BulkPermissionResponse> bulkGrantPermissions(
            @PathVariable Long connectionId,
            @Valid @RequestBody BulkPermissionRequest request
    ) {
        logger.info("Starting bulk permission grant for connection ID: {}, type: {}, scope: {}",
                connectionId, request.permissionType(), request.scope());

        // Convert DTO to Model
        cherry.mastermeister.model.BulkPermissionRequest modelRequest = new cherry.mastermeister.model.BulkPermissionRequest(
                request.scope(),
                request.permissionType(),
                request.userEmails(),
                request.schemaNames(),
                request.tableNames(),
                request.includeSystemTables(),
                request.description()
        );

        BulkPermissionResult modelResponse = permissionBulkService.grantBulkPermissions(connectionId, modelRequest);

        // Convert Model to DTO
        BulkPermissionResponse dtoResponse = new BulkPermissionResponse(
                modelResponse.processedUsers(),
                modelResponse.processedTables(),
                modelResponse.createdPermissions(),
                modelResponse.skippedExisting(),
                modelResponse.errors()
        );

        logger.info("Bulk permission grant completed: {} permissions created, {} errors",
                dtoResponse.createdPermissions(), dtoResponse.errors().size());

        return ApiResponse.success(dtoResponse);
    }

    @GetMapping("/{connectionId}/export")
    @Operation(summary = "Export permissions", description = "Export all permissions for a connection as YAML")
    public ResponseEntity<byte[]> exportPermissions(
            @PathVariable Long connectionId,
            @RequestParam(defaultValue = "Permission export") String description
    ) {

        logger.info("Exporting permissions for connection ID: {}", connectionId);

        String yamlContent = permissionYamlService.exportPermissionsAsYaml(connectionId, description);

        String filename = String.format("permissions-connection-%d.yml", connectionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/x-yaml"))
                .body(yamlContent.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/{connectionId}/import")
    @Operation(summary = "Import permissions", description = "Import permissions from YAML file")
    public ApiResponse<PermissionYamlService.PermissionImportResult> importPermissions(
            @PathVariable Long connectionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean importUsers,
            @RequestParam(defaultValue = "true") boolean importTemplates,
            @RequestParam(defaultValue = "false") boolean clearExistingPermissions,
            @RequestParam(defaultValue = "OVERWRITE") DuplicateHandling duplicateHandling
    ) {

        logger.info("Importing permissions for connection ID: {} from file: {}", connectionId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        try {
            String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            PermissionYamlService.ImportOptions options = new PermissionYamlService.ImportOptions(
                    importUsers, importTemplates, clearExistingPermissions, duplicateHandling);

            PermissionYamlService.PermissionImportResult result = permissionYamlService
                    .importPermissionsFromYaml(yamlContent, connectionId, options);

            logger.info("Import completed: {} users, {} templates, {} permissions imported",
                    result.importedUsers(), result.importedTemplates(), result.importedPermissions());

            return ApiResponse.success(result);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file", e);
        }
    }

    @PostMapping("/{connectionId}/validate")
    @Operation(summary = "Validate YAML", description = "Validate YAML permission configuration without importing")
    public ApiResponse<ValidationResponse> validateYaml(
            @PathVariable Long connectionId,
            @RequestParam("file") MultipartFile file
    ) {

        logger.info("Validating YAML for connection ID: {} from file: {}", connectionId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        try {
            String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            ValidationResponse result = validateYamlContent(yamlContent);

            return ApiResponse.success(result);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file", e);
        }
    }

    /**
     * Validate YAML content structure
     */
    private ValidationResponse validateYamlContent(String yamlContent) {
        try {
            // Use a simple approach - try to parse and count elements
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper(
                            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            yamlMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            PermissionExportData data =
                    yamlMapper.readValue(yamlContent, PermissionExportData.class);

            int userCount = data.users() != null ? data.users().size() : 0;
            int templateCount = data.templates() != null ? data.templates().size() : 0;
            int totalPermissions = 0;

            if (data.users() != null) {
                totalPermissions += data.users().stream()
                        .mapToInt(user -> user.permissions() != null ? user.permissions().size() : 0)
                        .sum();
            }

            if (data.templates() != null) {
                totalPermissions += data.templates().stream()
                        .mapToInt(template -> template.items() != null ? template.items().size() : 0)
                        .sum();
            }

            return new ValidationResponse(true, "YAML structure is valid", userCount, templateCount, totalPermissions);

        } catch (Exception e) {
            return new ValidationResponse(false, "Invalid YAML structure: " + e.getMessage(), 0, 0, 0);
        }
    }

    /**
     * Validation result record
     */
    public record ValidationResponse(
            boolean valid,
            String message,
            int userCount,
            int templateCount,
            int totalPermissions
    ) {
    }
}

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
import cherry.mastermeister.service.PermissionYamlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Permission Export/Import", description = "YAML-based permission configuration management")
@SecurityRequirement(name = "bearerAuth")
public class PermissionExportController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionYamlService permissionYamlService;

    public PermissionExportController(PermissionYamlService permissionYamlService) {
        this.permissionYamlService = permissionYamlService;
    }

    @GetMapping("/{connectionId}/export")
    @Operation(summary = "Export permissions", description = "Export all permissions for a connection as YAML")
    public ResponseEntity<byte[]> exportPermissions(
            @PathVariable Long connectionId,
            @RequestParam(defaultValue = "Permission export") String description) {
        
        logger.info("Exporting permissions for connection ID: {}", connectionId);

        try {
            String yamlContent = permissionYamlService.exportPermissionsAsYaml(connectionId, description);
            
            String filename = String.format("permissions-connection-%d.yml", connectionId);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/x-yaml"))
                    .body(yamlContent.getBytes(StandardCharsets.UTF_8));
                    
        } catch (Exception e) {
            logger.error("Failed to export permissions for connection: {}", connectionId, e);
            throw new RuntimeException("Permission export failed", e);
        }
    }

    @PostMapping("/{connectionId}/import")
    @Operation(summary = "Import permissions", description = "Import permissions from YAML file")
    public ResponseEntity<ApiResponse<PermissionYamlService.PermissionImportResult>> importPermissions(
            @PathVariable Long connectionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean importUsers,
            @RequestParam(defaultValue = "true") boolean importTemplates,
            @RequestParam(defaultValue = "false") boolean clearExistingPermissions,
            @RequestParam(defaultValue = "true") boolean skipDuplicates) {
        
        logger.info("Importing permissions for connection ID: {} from file: {}", connectionId, file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No file provided"));
            }

            String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            PermissionYamlService.ImportOptions options = new PermissionYamlService.ImportOptions(
                    importUsers, importTemplates, clearExistingPermissions, skipDuplicates);
            
            PermissionYamlService.PermissionImportResult result = permissionYamlService
                    .importPermissionsFromYaml(yamlContent, connectionId, options);
            
            logger.info("Import completed: {} users, {} templates, {} permissions imported", 
                    result.importedUsers(), result.importedTemplates(), result.importedPermissions());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Failed to import permissions for connection: {}", connectionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Permission import failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{connectionId}/validate")
    @Operation(summary = "Validate YAML", description = "Validate YAML permission configuration without importing")
    public ResponseEntity<ApiResponse<ValidationResult>> validateYaml(
            @PathVariable Long connectionId,
            @RequestParam("file") MultipartFile file) {
        
        logger.info("Validating YAML for connection ID: {} from file: {}", connectionId, file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No file provided"));
            }

            String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            ValidationResult result = validateYamlContent(yamlContent);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Failed to validate YAML for connection: {}", connectionId, e);
            return ResponseEntity.ok(ApiResponse.success(
                    new ValidationResult(false, "Validation failed: " + e.getMessage(), 0, 0, 0)));
        }
    }

    /**
     * Validate YAML content structure
     */
    private ValidationResult validateYamlContent(String yamlContent) {
        try {
            // Use a simple approach - try to parse and count elements
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper(
                            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            yamlMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            
            cherry.mastermeister.controller.dto.PermissionExportData data = 
                    yamlMapper.readValue(yamlContent, cherry.mastermeister.controller.dto.PermissionExportData.class);
            
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
            
            return new ValidationResult(true, "YAML structure is valid", userCount, templateCount, totalPermissions);
            
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid YAML structure: " + e.getMessage(), 0, 0, 0);
        }
    }

    /**
     * Validation result record
     */
    public record ValidationResult(
            boolean valid,
            String message,
            int userCount,
            int templateCount,
            int totalPermissions
    ) {}
}
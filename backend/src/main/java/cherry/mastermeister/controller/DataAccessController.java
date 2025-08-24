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

import cherry.mastermeister.annotation.RequirePermission;
import cherry.mastermeister.controller.dto.AccessibleTableDto;
import cherry.mastermeister.controller.dto.ApiResponse;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.service.DataAccessService;
import cherry.mastermeister.service.PermissionAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Data Access", description = "Secure data access with permission control")
@SecurityRequirement(name = "bearerAuth")
public class DataAccessController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DataAccessService dataAccessService;
    private final PermissionAuthService permissionAuthService;

    public DataAccessController(DataAccessService dataAccessService, PermissionAuthService permissionAuthService) {
        this.dataAccessService = dataAccessService;
        this.permissionAuthService = permissionAuthService;
    }

    @GetMapping("/{connectionId}/tables")
    @Operation(summary = "Get accessible tables", description = "Get tables accessible to current user with READ permission")
    @RequirePermission(value = PermissionType.READ, connectionIdParam = "connectionId")
    public ResponseEntity<ApiResponse<List<AccessibleTableDto>>> getAccessibleTables(
            @PathVariable Long connectionId,
            @RequestParam(defaultValue = "READ") String permissionType) {

        logger.info("Getting accessible tables for connection: {} with permission: {}", connectionId, permissionType);

        try {
            PermissionType permission = PermissionType.valueOf(permissionType.toUpperCase());
            List<TableMetadata> tables = dataAccessService.getAccessibleTables(connectionId, permission);

            // Convert to DTOs with permission information
            List<AccessibleTableDto> accessibleTables = tables.stream()
                    .map(table -> convertToAccessibleTableDto(table, connectionId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(accessibleTables));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid permission type: {}", permissionType);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid permission type: " + permissionType));
        } catch (Exception e) {
            logger.error("Failed to get accessible tables for connection: {}", connectionId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get accessible tables"));
        }
    }

    @GetMapping("/{connectionId}/tables/{schemaName}/{tableName}")
    @Operation(summary = "Get table information", description = "Get detailed information for a specific table")
    @RequirePermission(value = PermissionType.READ, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ResponseEntity<ApiResponse<AccessibleTableDto>> getTableInfo(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName) {

        logger.info("Getting table info for {}.{} on connection: {}", schemaName, tableName, connectionId);

        try {
            TableMetadata tableInfo = dataAccessService.getTableInfo(connectionId, schemaName, tableName);
            AccessibleTableDto accessibleTable = convertToAccessibleTableDto(tableInfo, connectionId);

            return ResponseEntity.ok(ApiResponse.success(accessibleTable));

        } catch (Exception e) {
            logger.error("Failed to get table info for {}.{} on connection: {}",
                    schemaName, tableName, connectionId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get table information"));
        }
    }

    /**
     * Convert TableInfo to AccessibleTableDto with permission information
     */
    private AccessibleTableDto convertToAccessibleTableDto(TableMetadata tableInfo, Long connectionId) {
        // Get user's permissions for this table
        Set<PermissionType> permissions = permissionAuthService.getUserTablePermissions(
                connectionId, tableInfo.schema(), tableInfo.tableName());

        // Convert permissions to string set
        Set<String> permissionStrings = permissions.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        // Create full table name
        String fullTableName = tableInfo.schema() != null && !tableInfo.schema().isEmpty()
                ? tableInfo.schema() + "." + tableInfo.tableName()
                : tableInfo.tableName();

        return new AccessibleTableDto(
                connectionId,
                tableInfo.schema(),
                tableInfo.tableName(),
                fullTableName,
                tableInfo.tableType(),
                tableInfo.comment(),
                permissionStrings,
                permissions.contains(PermissionType.READ),
                permissions.contains(PermissionType.WRITE),
                permissions.contains(PermissionType.DELETE),
                permissions.contains(PermissionType.ADMIN),
                permissions.contains(PermissionType.READ) && permissions.contains(PermissionType.WRITE),
                permissions.contains(PermissionType.READ) && permissions.contains(PermissionType.WRITE)
                        && permissions.contains(PermissionType.DELETE)
        );
    }
}

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
import cherry.mastermeister.controller.dto.*;
import cherry.mastermeister.model.RecordCreationResult;
import cherry.mastermeister.model.RecordUpdateResult;
import cherry.mastermeister.model.RecordDeleteResult;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.RecordFilter;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.service.DataAccessService;
import cherry.mastermeister.service.PermissionAuthService;
import cherry.mastermeister.service.RecordAccessService;
import cherry.mastermeister.service.RecordCreationService;
import cherry.mastermeister.service.RecordUpdateService;
import cherry.mastermeister.service.RecordDeleteService;
import cherry.mastermeister.service.RecordFilterConverterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
    private final RecordAccessService recordAccessService;
    private final RecordCreationService recordCreationService;
    private final RecordUpdateService recordUpdateService;
    private final RecordDeleteService recordDeleteService;
    private final RecordFilterConverterService recordFilterConverterService;

    public DataAccessController(
            DataAccessService dataAccessService,
            PermissionAuthService permissionAuthService,
            RecordAccessService recordAccessService,
            RecordCreationService recordCreationService,
            RecordUpdateService recordUpdateService,
            RecordDeleteService recordDeleteService,
            RecordFilterConverterService recordFilterConverterService
    ) {
        this.dataAccessService = dataAccessService;
        this.permissionAuthService = permissionAuthService;
        this.recordAccessService = recordAccessService;
        this.recordCreationService = recordCreationService;
        this.recordUpdateService = recordUpdateService;
        this.recordDeleteService = recordDeleteService;
        this.recordFilterConverterService = recordFilterConverterService;
    }

    @GetMapping("/{connectionId}/tables")
    @Operation(summary = "Get accessible tables", description = "Get tables accessible to current user with READ permission")
    @RequirePermission(value = PermissionType.READ, connectionIdParam = "connectionId")
    public ApiResponse<List<AccessibleTableResult>> getAccessibleTables(
            @PathVariable Long connectionId,
            @RequestParam(defaultValue = "READ") String permissionType
    ) {

        logger.info("Getting accessible tables for connection: {} with permission: {}", connectionId, permissionType);

        PermissionType permission = PermissionType.valueOf(permissionType.toUpperCase());
        List<TableMetadata> tables = dataAccessService.getAccessibleTables(connectionId, permission);

        // Convert to DTOs with permission information
        List<AccessibleTableResult> accessibleTables = tables.stream()
                .map(table -> convertToAccessibleTableDto(table, connectionId))
                .collect(Collectors.toList());

        return ApiResponse.success(accessibleTables);
    }

    @GetMapping("/{connectionId}/tables/{schemaName}/{tableName}")
    @Operation(summary = "Get table information", description = "Get detailed information for a specific table")
    @RequirePermission(value = PermissionType.READ, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ApiResponse<AccessibleTableResult> getTableInfo(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName
    ) {

        logger.info("Getting table info for {}.{} on connection: {}", schemaName, tableName, connectionId);

        TableMetadata tableInfo = dataAccessService.getTableInfo(connectionId, schemaName, tableName);
        AccessibleTableResult accessibleTable = convertToAccessibleTableDto(tableInfo, connectionId);

        return ApiResponse.success(accessibleTable);
    }

    @GetMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(summary = "Get table records", description = "Get records from table with column-level permission filtering")
    @RequirePermission(value = PermissionType.READ, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ApiResponse<RecordQueryResult> getTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {

        logger.info("Getting records for table {}.{} on connection: {}, page: {}, size: {}",
                schemaName, tableName, connectionId, page, pageSize);

        cherry.mastermeister.model.RecordQueryResult result = recordAccessService.getRecords(
                connectionId, schemaName, tableName, page, pageSize);

        RecordQueryResult dto = convertToRecordQueryResultDto(result);
        return ApiResponse.success(dto);
    }

    @PostMapping("/{connectionId}/tables/{schemaName}/{tableName}/records/search")
    @Operation(summary = "Search table records with filters", description = "Get filtered records from table with column-level permission filtering")
    @RequirePermission(value = PermissionType.READ, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ApiResponse<RecordQueryResult> searchTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestBody(required = false) RecordFilterRequest filterRequest
    ) {

        logger.info("Searching records for table {}.{} on connection: {}, page: {}, size: {}, filters: {}",
                schemaName, tableName, connectionId, page, pageSize,
                filterRequest != null ? "present" : "none");

        RecordFilter filter = recordFilterConverterService.convertFromRequest(filterRequest);

        cherry.mastermeister.model.RecordQueryResult result = recordAccessService.getRecords(
                connectionId, schemaName, tableName, filter, page, pageSize);

        RecordQueryResult dto = convertToRecordQueryResultDto(result);
        return ApiResponse.success(dto);
    }

    @PostMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(summary = "Create table record", description = "Create a new record in table with column-level permission validation")
    @RequirePermission(value = PermissionType.WRITE, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ResponseEntity<ApiResponse<RecordCreateResult>> createTableRecord(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody RecordCreateRequest request
    ) {

        logger.info("Creating record in table {}.{} on connection: {}", schemaName, tableName, connectionId);

        RecordCreationResult result = recordCreationService.createRecord(
                connectionId, schemaName, tableName, request.data());

        RecordCreateResult dto = convertToRecordCreateResult(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(summary = "Update table records", description = "Update records in table with column-level permission validation and transaction management")
    @RequirePermission(value = PermissionType.WRITE, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ApiResponse<cherry.mastermeister.controller.dto.RecordUpdateResult> updateTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody RecordUpdateRequest request
    ) {

        logger.info("Updating records in table {}.{} on connection: {}", schemaName, tableName, connectionId);

        cherry.mastermeister.model.RecordUpdateResult result = recordUpdateService.updateRecord(
                connectionId, schemaName, tableName, request.data(), request.whereConditions());

        cherry.mastermeister.controller.dto.RecordUpdateResult dto = convertToRecordUpdateResult(result);
        return ApiResponse.success(dto);
    }

    @DeleteMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(summary = "Delete table records", description = "Delete records from table with referential integrity checks and column-level permission validation")
    @RequirePermission(value = PermissionType.DELETE, connectionIdParam = "connectionId",
            schemaNameParam = "schemaName", tableNameParam = "tableName")
    public ApiResponse<cherry.mastermeister.controller.dto.RecordDeleteResult> deleteTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody RecordDeleteRequest request
    ) {

        logger.info("Deleting records from table {}.{} on connection: {}", schemaName, tableName, connectionId);

        cherry.mastermeister.model.RecordDeleteResult result = recordDeleteService.deleteRecord(
                connectionId, schemaName, tableName, request.whereConditions(), 
                request.skipReferentialIntegrityCheck());

        cherry.mastermeister.controller.dto.RecordDeleteResult dto = convertToRecordDeleteResult(result);
        return ApiResponse.success(dto);
    }

    /**
     * Convert model RecordDeleteResult to DTO
     */
    private cherry.mastermeister.controller.dto.RecordDeleteResult convertToRecordDeleteResult(cherry.mastermeister.model.RecordDeleteResult model) {
        return new cherry.mastermeister.controller.dto.RecordDeleteResult(
                model.deletedRecordCount(),
                model.executionTimeMs(),
                model.query(),
                model.referentialIntegrityChecked(),
                model.warnings()
        );
    }

    /**
     * Convert model RecordUpdateResult to DTO
     */
    private cherry.mastermeister.controller.dto.RecordUpdateResult convertToRecordUpdateResult(cherry.mastermeister.model.RecordUpdateResult model) {
        return new cherry.mastermeister.controller.dto.RecordUpdateResult(
                model.updatedRecordCount(),
                model.executionTimeMs(),
                model.query()
        );
    }

    /**
     * Convert model RecordCreationResult to DTO
     */
    private RecordCreateResult convertToRecordCreateResult(RecordCreationResult model) {
        return new RecordCreateResult(
                model.createdRecord(),
                model.columnTypes(),
                model.executionTimeMs(),
                model.query()
        );
    }

    /**
     * Convert model RecordQueryResult to DTO
     */
    private RecordQueryResult convertToRecordQueryResultDto(
            cherry.mastermeister.model.RecordQueryResult model
    ) {
        // Convert records to readable data only
        List<Map<String, Object>> records = model.records().stream()
                .map(record -> record.getReadableData())
                .collect(Collectors.toList());

        // Convert column metadata
        List<ColumnMetadataResult> columns = model.accessibleColumns().stream()
                .map(this::convertToColumnMetadataResult)
                .collect(Collectors.toList());

        return new RecordQueryResult(
                records,
                columns,
                model.totalRecords(),
                model.currentPage(),
                model.pageSize(),
                model.getTotalPages(),
                model.hasNextPage(),
                model.hasPreviousPage(),
                model.executionTimeMs(),
                model.query()
        );
    }

    /**
     * Convert ColumnMetadata to ColumnMetadataResult
     */
    private ColumnMetadataResult convertToColumnMetadataResult(
            ColumnMetadata column
    ) {
        return new ColumnMetadataResult(
                column.columnName(),
                column.dataType(),
                column.columnSize(),
                column.decimalDigits(),
                column.nullable(),
                column.defaultValue(),
                column.comment(),
                column.primaryKey(),
                column.autoIncrement(),
                column.ordinalPosition()
        );
    }

    /**
     * Convert TableInfo to AccessibleTableDto with permission information
     */
    private AccessibleTableResult convertToAccessibleTableDto(
            TableMetadata tableInfo, Long connectionId
    ) {
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

        return new AccessibleTableResult(
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

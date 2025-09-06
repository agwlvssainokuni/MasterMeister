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

import cherry.mastermeister.controller.dto.*;
import cherry.mastermeister.controller.dto.RecordCreateResponse;
import cherry.mastermeister.controller.dto.RecordDeleteResponse;
import cherry.mastermeister.controller.dto.RecordQueryResponse;
import cherry.mastermeister.controller.dto.RecordUpdateResponse;
import cherry.mastermeister.exception.UserNotFoundException;
import cherry.mastermeister.model.*;
import cherry.mastermeister.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@Tag(name = "Data Access", description = "Secure data access with permission control")
@SecurityRequirement(name = "bearerAuth")
public class DataAccessController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AccessibleTableService accessibleTableService;
    private final RecordReadService recordReadService;
    private final RecordCreateService recordCreateService;
    private final RecordUpdateService recordUpdateService;
    private final RecordDeleteService recordDeleteService;
    private final RecordFilterConverterService recordFilterConverterService;
    private final DatabaseService databaseService;
    private final UserService userService;

    public DataAccessController(
            AccessibleTableService accessibleTableService,
            RecordReadService recordReadService,
            RecordCreateService recordCreateService,
            RecordUpdateService recordUpdateService,
            RecordDeleteService recordDeleteService,
            RecordFilterConverterService recordFilterConverterService,
            DatabaseService databaseService,
            UserService userService
    ) {
        this.accessibleTableService = accessibleTableService;
        this.recordReadService = recordReadService;
        this.recordCreateService = recordCreateService;
        this.recordUpdateService = recordUpdateService;
        this.recordDeleteService = recordDeleteService;
        this.recordFilterConverterService = recordFilterConverterService;
        this.databaseService = databaseService;
        this.userService = userService;
    }

    @GetMapping("/databases")
    @Operation(
            summary = "Get available databases",
            description = "Get list of active database connections available to the user"
    )
    public ApiResponse<List<DatabaseConnectionResponse>> getAvailableDatabases() {
        logger.info("Getting available databases for current user");

        List<DatabaseConnection> databases = databaseService.getAllConnections();

        List<DatabaseConnectionResponse> databaseResults = databases.stream()
                .map(this::convertToDatabaseConnectionResult)
                .collect(Collectors.toList());

        return ApiResponse.success(databaseResults);
    }

    @GetMapping("/{connectionId}/tables")
    @Operation(
            summary = "Get all tables with permission info",
            description = "Get all table metadata with user permission information for each table"
    )
    public ApiResponse<List<AccessibleTableResponse>> getAccessibleTables(
            @PathVariable Long connectionId,
            Authentication authentication
    ) {

        logger.info("Getting all tables with permission info for connection: {}", connectionId);

        Long userId = getUserId(authentication);
        // Get all tables with permission information (metadata should be visible to all)
        List<AccessibleTable> tables = accessibleTableService.getAllAvailableTables(
                userId, connectionId
        );

        // Convert to DTOs with permission information (without column details for performance)
        List<AccessibleTableResponse> accessibleTables = tables.stream()
                .map(table -> convertAccessibleTableToDto(
                        userId, table,
                        false
                ))
                .collect(Collectors.toList());

        return ApiResponse.success(accessibleTables);
    }

    @GetMapping("/{connectionId}/tables/{schemaName}/{tableName}")
    @Operation(
            summary = "Get table details",
            description = "Get detailed information for a specific table"
    )
    public ApiResponse<AccessibleTableResponse> getTableDetails(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            Authentication authentication
    ) {

        logger.info("Getting table details for {}.{} on connection: {}", schemaName, tableName, connectionId);

        Long userId = getUserId(authentication);
        AccessibleTable accessibleTable = accessibleTableService.getAccessibleTableWithColumns(
                userId, connectionId,
                schemaName, tableName
        );
        AccessibleTableResponse result = convertAccessibleTableToDto(
                userId, accessibleTable,
                true
        );

        return ApiResponse.success(result);
    }

    @GetMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(
            summary = "Get table records",
            description = "Get records from table with column-level permission filtering"
    )
    public ApiResponse<RecordQueryResponse> getTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            Authentication authentication
    ) {

        logger.info("Getting records for table {}.{} on connection: {}, page: {}, size: {}",
                schemaName, tableName, connectionId, page, pageSize);

        Long userId = getUserId(authentication);
        cherry.mastermeister.model.RecordQueryResponse result = recordReadService.getRecords(
                userId, connectionId,
                schemaName, tableName,
                RecordFilter.empty(),
                page, pageSize
        );

        RecordQueryResponse dto = convertToRecordQueryResultDto(result);
        return ApiResponse.success(dto);
    }

    @PostMapping("/{connectionId}/tables/{schemaName}/{tableName}/records/filter")
    @Operation(
            summary = "Filter table records",
            description = "Get filtered records from table with column-level permission filtering"
    )
    public ApiResponse<RecordQueryResponse> filterTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestBody(required = false) RecordFilterSpec filterRequest,
            Authentication authentication
    ) {

        logger.info("Filtering records for table {}.{} on connection: {}, page: {}, size: {}, filters: {}",
                schemaName, tableName, connectionId, page, pageSize,
                filterRequest != null ? "present" : "none");

        RecordFilter filter = recordFilterConverterService.convertFromRequest(filterRequest);

        Long userId = getUserId(authentication);
        cherry.mastermeister.model.RecordQueryResponse result = recordReadService.getRecords(
                userId, connectionId,
                schemaName, tableName,
                filter,
                page, pageSize
        );

        RecordQueryResponse dto = convertToRecordQueryResultDto(result);
        return ApiResponse.success(dto);
    }

    @PostMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(
            summary = "Create table record",
            description = "Create a new record in table with column-level permission validation"
    )
    public ResponseEntity<ApiResponse<RecordCreateResponse>> createTableRecord(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody RecordCreateSpec request,
            Authentication authentication
    ) {

        logger.info("Creating record in table {}.{} on connection: {}", schemaName, tableName, connectionId);

        Long userId = getUserId(authentication);
        cherry.mastermeister.model.RecordCreateResponse result = recordCreateService.createRecord(
                userId, connectionId,
                schemaName, tableName,
                request.data()
        );

        RecordCreateResponse dto = convertToRecordCreateResult(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{connectionId}/tables/{schemaName}/{tableName}/records")
    @Operation(
            summary = "Update table records",
            description = "Update records in table with column-level permission validation and transaction management"
    )
    public ApiResponse<RecordUpdateResponse> updateTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody RecordUpdateSpec request,
            Authentication authentication
    ) {

        logger.info("Updating records in table {}.{} on connection: {}", schemaName, tableName, connectionId);

        Long userId = getUserId(authentication);
        cherry.mastermeister.model.RecordUpdateResponse result = recordUpdateService.updateRecord(
                userId, connectionId,
                schemaName, tableName,
                request.data(),
                request.whereConditions()
        );

        RecordUpdateResponse dto = convertToRecordUpdateResult(result);
        return ApiResponse.success(dto);
    }

    @PostMapping("/{connectionId}/tables/{schemaName}/{tableName}/records:delete")
    @Operation(
            summary = "Delete table records",
            description = "Delete records from table with referential integrity checks and column-level permission validation"
    )
    public ApiResponse<RecordDeleteResponse> deleteTableRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody RecordDeleteSpec request,
            Authentication authentication
    ) {

        logger.info("Deleting records from table {}.{} on connection: {}", schemaName, tableName, connectionId);

        Long userId = getUserId(authentication);
        cherry.mastermeister.model.RecordDeleteResponse result = recordDeleteService.deleteRecord(
                userId, connectionId,
                schemaName, tableName,
                request.whereConditions(),
                request.skipReferentialIntegrityCheck()
        );

        RecordDeleteResponse dto = convertToRecordDeleteResult(result);
        return ApiResponse.success(dto);
    }

    /**
     * Convert model RecordDeleteResponse to DTO
     */
    private RecordDeleteResponse convertToRecordDeleteResult(
            cherry.mastermeister.model.RecordDeleteResponse model
    ) {
        return new RecordDeleteResponse(
                model.deletedRecordCount(),
                model.executionTimeMs(),
                model.query(),
                model.referentialIntegrityChecked(),
                model.warnings()
        );
    }

    /**
     * Convert model RecordUpdateResponse to DTO
     */
    private RecordUpdateResponse convertToRecordUpdateResult(
            cherry.mastermeister.model.RecordUpdateResponse model
    ) {
        return new RecordUpdateResponse(
                model.updatedRecordCount(),
                model.executionTimeMs(),
                model.query()
        );
    }

    /**
     * Convert model RecordCreationResult to DTO
     */
    private cherry.mastermeister.controller.dto.RecordCreateResponse convertToRecordCreateResult(
            cherry.mastermeister.model.RecordCreateResponse model
    ) {
        return new cherry.mastermeister.controller.dto.RecordCreateResponse(
                model.createdRecord(),
                model.columnTypes(),
                model.executionTimeMs(),
                model.query()
        );
    }

    /**
     * Convert model RecordQueryResponse to DTO
     */
    private RecordQueryResponse convertToRecordQueryResultDto(
            cherry.mastermeister.model.RecordQueryResponse model
    ) {
        // Convert records to readable data only
        List<Map<String, Object>> records = model.records().stream()
                .map(TableRecord::getReadableData)
                .collect(Collectors.toList());

        // Convert column metadata
        List<AccessibleColumnResponse> columns = model.accessibleColumns().stream()
                .map(this::convertAccessibleColumnToResult)
                .collect(Collectors.toList());

        return new RecordQueryResponse(
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
     * Convert AccessibleTable to AccessibleTableResponse DTO
     */
    private AccessibleTableResponse convertAccessibleTableToDto(
            Long userId,
            AccessibleTable accessibleTable,
            boolean includeColumns
    ) {
        // Convert permissions to string set
        Set<String> permissionStrings = accessibleTable.permissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        // Use columns from AccessibleTable if available, or fetch if requested
        List<AccessibleColumnResponse> columnResults;
        if (includeColumns) {
            if (accessibleTable.columns() != null) {
                // Convert AccessibleColumn to AccessibleColumnResponse
                columnResults = accessibleTable.columns().stream()
                        .map(this::convertAccessibleColumnToResult)
                        .collect(Collectors.toList());
            } else {
                // Fetch columns if not included in AccessibleTable
                columnResults = getColumnsWithPermissions(
                        userId, accessibleTable.connectionId(),
                        accessibleTable.schemaName(), accessibleTable.tableName()
                );
            }
        } else {
            columnResults = List.of(); // Empty list for performance when not needed
        }

        return new AccessibleTableResponse(
                accessibleTable.connectionId(),
                accessibleTable.schemaName(),
                accessibleTable.tableName(),
                accessibleTable.getFullTableName(),
                accessibleTable.tableType(),
                accessibleTable.comment(),
                permissionStrings,
                accessibleTable.canRead(),
                accessibleTable.canWrite(),
                accessibleTable.canDelete(),
                accessibleTable.canAdmin(),
                accessibleTable.canModifyData(),
                accessibleTable.canPerformCrud(),
                columnResults
        );
    }

    /**
     * Convert DatabaseConnection model to DatabaseConnectionResponse DTO
     */
    private DatabaseConnectionResponse convertToDatabaseConnectionResult(
            cherry.mastermeister.model.DatabaseConnection connection
    ) {
        return new DatabaseConnectionResponse(
                connection.id(),
                connection.name(),
                connection.dbType(),
                connection.host(),
                connection.port(),
                connection.databaseName(),
                connection.username(),
                connection.connectionParams(),
                connection.active(),
                connection.lastTestedAt(),
                connection.testResult(),
                connection.createdAt(),
                connection.updatedAt()
        );
    }

    /**
     * Get columns with permission information for AccessibleTable conversion
     */
    private List<AccessibleColumnResponse> getColumnsWithPermissions(
            Long userId, Long connectionId,
            String schemaName, String tableName
    ) {
        // Get accessible table with column information
        AccessibleTable accessibleTable = accessibleTableService.getAccessibleTableWithColumns(
                userId, connectionId,
                schemaName, tableName
        );

        return accessibleTable.columns().stream()
                .map(this::convertAccessibleColumnToResult)
                .collect(Collectors.toList());
    }

    /**
     * Get current authenticated user ID
     */
    private Long getUserId(Authentication authentication) {
        return Optional.of(authentication)
                .map(Authentication::getName)
                .flatMap(userService::getUserIdByEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + authentication.getName()));
    }

    /**
     * Convert AccessibleColumn to AccessibleColumnResponse
     */
    private AccessibleColumnResponse convertAccessibleColumnToResult(AccessibleColumn column) {
        // Convert permissions to string set
        Set<String> permissionStrings = column.permissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new AccessibleColumnResponse(
                column.columnName(),
                column.dataType(),
                column.columnSize(),
                column.decimalDigits(),
                column.nullable(),
                column.defaultValue(),
                column.comment(),
                column.primaryKey(),
                column.autoIncrement(),
                column.ordinalPosition(),
                permissionStrings,
                column.canRead(),
                column.canWrite(),
                column.canDelete(),
                column.canAdmin()
        );
    }
}

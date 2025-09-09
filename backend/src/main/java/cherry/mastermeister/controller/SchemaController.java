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
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.SchemaUpdateLog;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.service.SchemaUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/schema")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Schema Management", description = "Database schema reading and metadata operations")
@SecurityRequirement(name = "bearerAuth")
public class SchemaController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SchemaUpdateService schemaUpdateService;

    public SchemaController(
            SchemaUpdateService schemaUpdateService
    ) {
        this.schemaUpdateService = schemaUpdateService;
    }

    @GetMapping("/{connectionId}")
    @Operation(summary = "Get cached schema metadata", description = "Get cached schema metadata for a database connection. Returns 204 No Content if no cache exists.")
    public ResponseEntity<ApiResponse<SchemaMetadataResponse>> getSchema(
            @PathVariable Long connectionId,
            Authentication authentication
    ) {
        logger.info("Getting cached schema metadata for connection ID: {}", connectionId);

        String userEmail = getUserEmail(authentication);
        Optional<SchemaMetadata> schemaOpt = schemaUpdateService.getSchema(connectionId);

        if (schemaOpt.isEmpty()) {
            logger.debug("No cached schema found for connection ID: {}, returning 204 No Content", connectionId);
            return ResponseEntity.noContent().build();
        }

        SchemaMetadataResponse result = toDto(schemaOpt.get());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{connectionId}/refresh")
    @Operation(summary = "Refresh schema metadata", description = "Force refresh schema metadata from database")
    public ApiResponse<SchemaMetadataResponse> refreshSchema(
            @PathVariable Long connectionId,
            Authentication authentication
    ) {
        logger.info("Refreshing schema metadata for connection ID: {}", connectionId);

        String userEmail = getUserEmail(authentication);
        SchemaMetadata schema = schemaUpdateService.refreshSchema(connectionId, userEmail);
        SchemaMetadataResponse result = toDto(schema);

        return ApiResponse.success(result);
    }

    @GetMapping("/{connectionId}/history")
    @Operation(summary = "Get operation history", description = "Get schema operation history for a connection")
    public ApiResponse<List<SchemaUpdateLogResponse>> getOperationHistory(
            @PathVariable Long connectionId
    ) {
        logger.info("Getting operation history for connection ID: {}", connectionId);

        List<SchemaUpdateLog> history = schemaUpdateService.getConnectionOperationHistory(connectionId);
        List<SchemaUpdateLogResponse> results = history.stream().map(this::toDto).toList();

        return ApiResponse.success(results);
    }

    @GetMapping("/{connectionId}/failures")
    @Operation(summary = "Get failed operations", description = "Get failed schema operations for a connection")
    public ApiResponse<List<SchemaUpdateLogResponse>> getFailedOperations(
            @PathVariable Long connectionId
    ) {
        logger.info("Getting failed operations for connection ID: {}", connectionId);

        List<SchemaUpdateLog> failures = schemaUpdateService.getFailedOperations(connectionId);
        List<SchemaUpdateLogResponse> results = failures.stream().map(this::toDto).toList();

        return ApiResponse.success(results);
    }

    private SchemaMetadataResponse toDto(SchemaMetadata model) {
        return new SchemaMetadataResponse(
                model.connectionId(),
                model.databaseName(),
                model.schemas(),
                model.tables().stream().map(this::toDto).toList(),
                model.lastUpdatedAt()
        );
    }

    private TableMetadataResponse toDto(TableMetadata model) {
        return new TableMetadataResponse(
                model.schema(),
                model.tableName(),
                model.tableType(),
                model.comment(),
                model.columns().stream().map(this::toDto).toList()
        );
    }

    private ColumnMetadataResponse toDto(ColumnMetadata model) {
        return new ColumnMetadataResponse(
                model.columnName(),
                model.dataType(),
                model.columnSize(),
                model.decimalDigits(),
                model.nullable(),
                model.defaultValue(),
                model.comment(),
                model.primaryKey(),
                model.autoIncrement(),
                model.ordinalPosition()
        );
    }

    private SchemaUpdateLogResponse toDto(SchemaUpdateLog model) {
        return new SchemaUpdateLogResponse(
                model.id(),
                model.connectionId(),
                model.operation(),
                model.userEmail(),
                model.executionTimeMs(),
                model.success(),
                model.errorMessage(),
                model.tablesCount(),
                model.columnsCount(),
                model.details(),
                model.createdAt()
        );
    }

    private String getUserEmail(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "system";
    }
}

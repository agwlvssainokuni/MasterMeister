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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Read schema metadata", description = "Read schema metadata for a database connection")
    public ApiResponse<SchemaMetadataResult> readSchema(@PathVariable Long connectionId) {
        logger.info("Reading schema metadata for connection ID: {}", connectionId);

        SchemaMetadata schema = schemaUpdateService.executeSchemaRead(connectionId);
        SchemaMetadataResult result = toDto(schema);

        return ApiResponse.success(result);
    }

    @GetMapping("/{connectionId}/cached")
    @Operation(summary = "Get cached schema metadata", description = "Get cached schema metadata from storage")
    public ApiResponse<SchemaMetadataResult> getCachedSchema(@PathVariable Long connectionId) {
        logger.info("Getting cached schema metadata for connection ID: {}", connectionId);

        Optional<SchemaMetadata> schema = schemaUpdateService.getStoredSchema(connectionId);
        if (schema.isEmpty()) {
            throw new RuntimeException("Cached schema not found for connection: " + connectionId);
        }

        SchemaMetadataResult result = toDto(schema.get());
        return ApiResponse.success(result);
    }

    @PostMapping("/{connectionId}/refresh")
    @Operation(summary = "Refresh schema metadata", description = "Force refresh schema metadata from database")
    public ApiResponse<SchemaMetadataResult> refreshSchema(@PathVariable Long connectionId) {
        logger.info("Refreshing schema metadata for connection ID: {}", connectionId);

        SchemaMetadata schema = schemaUpdateService.executeSchemaRefresh(connectionId);
        SchemaMetadataResult result = toDto(schema);

        return ApiResponse.success(result);
    }

    @GetMapping("/{connectionId}/history")
    @Operation(summary = "Get operation history", description = "Get schema operation history for a connection")
    public ApiResponse<List<SchemaUpdateLogResult>> getOperationHistory(@PathVariable Long connectionId) {
        logger.info("Getting operation history for connection ID: {}", connectionId);

        List<SchemaUpdateLog> history = schemaUpdateService.getConnectionOperationHistory(connectionId);
        List<SchemaUpdateLogResult> results = history.stream().map(this::toDto).toList();

        return ApiResponse.success(results);
    }

    @GetMapping("/{connectionId}/failures")
    @Operation(summary = "Get failed operations", description = "Get failed schema operations for a connection")
    public ApiResponse<List<SchemaUpdateLogResult>> getFailedOperations(@PathVariable Long connectionId) {
        logger.info("Getting failed operations for connection ID: {}", connectionId);

        List<SchemaUpdateLog> failures = schemaUpdateService.getFailedOperations(connectionId);
        List<SchemaUpdateLogResult> results = failures.stream().map(this::toDto).toList();

        return ApiResponse.success(results);
    }

    private SchemaMetadataResult toDto(SchemaMetadata model) {
        return new SchemaMetadataResult(
                model.connectionId(),
                model.databaseName(),
                model.schemas(),
                model.tables().stream().map(this::toDto).toList(),
                model.lastUpdatedAt()
        );
    }

    private TableMetadataResult toDto(TableMetadata model) {
        return new TableMetadataResult(
                model.schema(),
                model.tableName(),
                model.tableType(),
                model.comment(),
                model.columns().stream().map(this::toDto).toList()
        );
    }

    private ColumnMetadataResult toDto(ColumnMetadata model) {
        return new ColumnMetadataResult(
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

    private SchemaUpdateLogResult toDto(SchemaUpdateLog model) {
        return new SchemaUpdateLogResult(
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
}

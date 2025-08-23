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
import cherry.mastermeister.controller.dto.ColumnMetadataResult;
import cherry.mastermeister.controller.dto.SchemaMetadataResult;
import cherry.mastermeister.controller.dto.TableMetadataResult;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.service.SchemaReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/schema")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Schema Management", description = "Database schema reading and metadata operations")
@SecurityRequirement(name = "bearerAuth")
public class SchemaController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SchemaReaderService schemaReaderService;

    public SchemaController(SchemaReaderService schemaReaderService) {
        this.schemaReaderService = schemaReaderService;
    }

    @GetMapping("/connections/{connectionId}")
    @Operation(summary = "Read schema metadata", description = "Read schema metadata for a database connection")
    public ResponseEntity<ApiResponse<SchemaMetadataResult>> readSchema(@PathVariable Long connectionId) {
        logger.info("Reading schema metadata for connection ID: {}", connectionId);

        SchemaMetadata schema = schemaReaderService.readSchema(connectionId);
        SchemaMetadataResult result = toDto(schema);

        return ResponseEntity.ok(ApiResponse.success(result));
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
}

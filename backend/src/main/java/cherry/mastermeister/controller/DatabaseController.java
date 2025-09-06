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
import cherry.mastermeister.controller.dto.DatabaseConnectionSpec;
import cherry.mastermeister.controller.dto.DatabaseConnectionResponse;
import cherry.mastermeister.controller.dto.ValidationGroups;
import cherry.mastermeister.model.DatabaseConnection;
import cherry.mastermeister.service.DatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/databases")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Database Connection Management", description = "APIs for managing database connections")
public class DatabaseController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseService databaseService;

    public DatabaseController(
            DatabaseService databaseService
    ) {
        this.databaseService = databaseService;
    }

    @GetMapping
    @Operation(summary = "Get all database connections", description = "Retrieve list of all database connections")
    public ApiResponse<List<DatabaseConnectionResponse>> getAllConnections() {
        List<DatabaseConnection> connections = databaseService.getAllConnections();
        List<DatabaseConnectionResponse> results = connections.stream()
                .map(this::toResult)
                .toList();
        return ApiResponse.success(results);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get database connection by ID", description = "Retrieve a specific database connection")
    public ApiResponse<DatabaseConnectionResponse> getConnection(
            @PathVariable Long id
    ) {
        DatabaseConnection connection = databaseService.getConnection(id);
        return ApiResponse.success(toResult(connection));
    }

    @PostMapping
    @Operation(summary = "Create database connection", description = "Create a new database connection")
    public ApiResponse<DatabaseConnectionResponse> createConnection(
            @Validated(ValidationGroups.Create.class) @RequestBody DatabaseConnectionSpec request
    ) {
        DatabaseConnection model = toModel(request);
        DatabaseConnection savedConnection = databaseService.createConnection(model);
        logger.info("Created database connection: {} (ID: {})", savedConnection.name(), savedConnection.id());
        return ApiResponse.success(toResult(savedConnection));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update database connection", description = "Update an existing database connection")
    public ApiResponse<DatabaseConnectionResponse> updateConnection(
            @PathVariable Long id,
            @Validated(ValidationGroups.Update.class) @RequestBody DatabaseConnectionSpec request
    ) {
        // 更新時はパスワードが空の場合、既存のパスワードを保持（DatabaseServiceで処理）
        DatabaseConnection model = toModel(request);
        DatabaseConnection updatedConnection = databaseService.updateConnection(id, model);
        logger.info("Updated database connection: {} (ID: {})", updatedConnection.name(), id);
        return ApiResponse.success(toResult(updatedConnection));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete database connection", description = "Delete a database connection")
    public ApiResponse<Void> deleteConnection(
            @PathVariable Long id
    ) {
        databaseService.deleteConnection(id);
        logger.info("Deleted database connection ID: {}", id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test database connection", description = "Test connectivity for a specific database connection")
    public ApiResponse<Map<String, Object>> testConnection(
            @PathVariable Long id
    ) {
        Map<String, Object> result = databaseService.testConnectionWithDetails(id);
        boolean isConnected = (Boolean) result.get("connected");
        logger.info("Connection test for ID {}: {}", id, isConnected ? "SUCCESS" : "FAILED");
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate database connection", description = "Activate a database connection")
    public ApiResponse<DatabaseConnectionResponse> activateConnection(
            @PathVariable Long id
    ) {
        DatabaseConnection connection = databaseService.activateConnection(id);
        logger.info("Activated database connection: {} (ID: {})", connection.name(), id);
        return ApiResponse.success(toResult(connection));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate database connection", description = "Deactivate a database connection")
    public ApiResponse<DatabaseConnectionResponse> deactivateConnection(
            @PathVariable Long id
    ) {
        DatabaseConnection connection = databaseService.deactivateConnection(id);
        logger.info("Deactivated database connection: {} (ID: {})", connection.name(), id);
        return ApiResponse.success(toResult(connection));
    }

    private DatabaseConnection toModel(DatabaseConnectionSpec request) {
        return new DatabaseConnection(
                null,
                request.name(),
                request.dbType(),
                request.host(),
                request.port(),
                request.databaseName(),
                request.username(),
                request.password(),
                request.connectionParams(),
                request.active(),
                null,
                null,
                null,
                null
        );
    }

    private DatabaseConnectionResponse toResult(DatabaseConnection model) {
        return new DatabaseConnectionResponse(
                model.id(),
                model.name(),
                model.dbType(),
                model.host(),
                model.port(),
                model.databaseName(),
                model.username(),
                model.connectionParams(),
                model.active(),
                model.lastTestedAt(),
                model.testResult(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}

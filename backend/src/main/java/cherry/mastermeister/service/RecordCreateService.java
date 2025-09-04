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

import cherry.mastermeister.enums.DatabaseType;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.DatabaseConnection;
import cherry.mastermeister.model.RecordCreateResult;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.util.SqlEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecordCreateService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseService databaseService;
    private final SchemaMetadataService schemaMetadataService;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    public RecordCreateService(
            DatabaseService databaseService,
            SchemaMetadataService schemaMetadataService,
            PermissionService permissionService,
            AuditLogService auditLogService
    ) {
        this.databaseService = databaseService;
        this.schemaMetadataService = schemaMetadataService;
        this.permissionService = permissionService;
        this.auditLogService = auditLogService;
    }

    /**
     * Create new record with permission validation
     */
    public RecordCreateResult createRecord(
            Long connectionId, String schemaName, String tableName, Map<String, Object> recordData
    ) {
        logger.info("Creating record in table {}.{} on connection: {}", schemaName, tableName, connectionId);

        long startTime = System.currentTimeMillis();

        // Get columns that user can write to
        List<String> writableColumnNames = permissionService.getWritableColumns(connectionId, schemaName, tableName);
        if (writableColumnNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "No writable columns found for table " + schemaName + "." + tableName);
        }

        try {
            // Get table metadata
            TableMetadata tableMetadata = schemaMetadataService.getTableMetadata(
                    connectionId, schemaName, tableName);

            // Filter record data to only include writable columns
            Map<String, Object> validatedData = filterWritableData(recordData, writableColumnNames, tableMetadata);

            // Build and execute INSERT query
            DataSource dataSource = databaseService.getDataSource(connectionId);
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            // Get database type for proper SQL escaping
            DatabaseConnection dbConnection = databaseService.getConnection(connectionId);
            DatabaseType dbType = dbConnection.dbType();

            InsertQueryResult insertQuery = buildInsertQuery(schemaName, tableName, validatedData, dbType);

            logger.debug("Executing INSERT query: {}", insertQuery.query());
            logger.debug("Query parameters: {}", insertQuery.parameters());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            int rowsAffected = namedJdbcTemplate.update(
                    insertQuery.query(),
                    insertQuery.parameterSource(),
                    keyHolder
            );

            if (rowsAffected != 1) {
                throw new RuntimeException("Expected 1 row to be inserted, but " + rowsAffected + " rows were affected");
            }

            // Get writable columns metadata for response
            List<ColumnMetadata> writableColumnsMetadata = getWritableColumnsMetadata(writableColumnNames, tableMetadata);

            // Get the created record with generated keys
            Map<String, Object> createdRecord = getCreatedRecordData(validatedData, keyHolder, writableColumnsMetadata);
            Map<String, String> columnTypes = writableColumnsMetadata.stream()
                    .collect(Collectors.toMap(ColumnMetadata::columnName, ColumnMetadata::dataType));

            long executionTime = System.currentTimeMillis() - startTime;

            RecordCreateResult result = new RecordCreateResult(
                    createdRecord, columnTypes, executionTime, insertQuery.query());

            // Log detailed record creation success
            auditLogService.logDataModificationDetailed(connectionId, schemaName, tableName,
                    "CREATE", 1, executionTime, insertQuery.query(),
                    String.format("Created record with %d columns", validatedData.size()));

            logger.info("Successfully created record in {}.{} in {}ms", schemaName, tableName, executionTime);

            return result;

        } catch (DataAccessException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log detailed database failure (REQUIRES_NEW ensures this is recorded even if transaction rolls back)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "CREATE", e.getMessage(), executionTime,
                    recordData != null ? "INSERT with " + recordData.size() + " fields" : "INSERT failed");

            logger.error("Failed to create record in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to create record", e);
        } catch (IllegalArgumentException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log validation failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "CREATE", "Validation failed: " + e.getMessage(), executionTime,
                    "Data validation or permission check failed");

            logger.error("Validation error creating record in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log unexpected failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "CREATE", "Unexpected error: " + e.getMessage(), executionTime,
                    "Internal server error during record creation");

            logger.error("Unexpected error creating record in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to create record due to internal error", e);
        }
    }

    /**
     * Filter input data to only include writable columns
     */
    private Map<String, Object> filterWritableData(
            Map<String, Object> inputData, List<String> writableColumnNames, TableMetadata tableMetadata
    ) {
        Map<String, Object> validatedData = new HashMap<>();

        for (String columnName : writableColumnNames) {
            // Find column metadata
            ColumnMetadata column = tableMetadata.columns().stream()
                    .filter(col -> col.columnName().equals(columnName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnName));

            // Skip auto-increment columns - they will be generated by database
            if (column.autoIncrement()) {
                continue;
            }

            if (inputData.containsKey(columnName)) {
                Object value = inputData.get(columnName);

                // Basic validation - null check for NOT NULL columns
                if (value == null && !column.nullable()) {
                    throw new IllegalArgumentException(
                            "Column '" + columnName + "' is required (NOT NULL)");
                }

                validatedData.put(columnName, value);
            } else if (!column.nullable() && column.defaultValue() == null) {
                // Required column with no default value
                throw new IllegalArgumentException(
                        "Column '" + columnName + "' is required but not provided");
            }
        }

        return validatedData;
    }

    /**
     * Get writable columns metadata for response
     */
    private List<ColumnMetadata> getWritableColumnsMetadata(
            List<String> writableColumnNames, TableMetadata tableMetadata
    ) {
        return writableColumnNames.stream()
                .map(columnName -> tableMetadata.columns().stream()
                        .filter(col -> col.columnName().equals(columnName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnName)))
                .collect(Collectors.toList());
    }

    /**
     * Build INSERT query with parameterized values
     */
    private InsertQueryResult buildInsertQuery(
            String schemaName, String tableName, Map<String, Object> data, DatabaseType dbType
    ) {
        String fullTableName = buildTableName(schemaName, tableName, dbType);

        if (data.isEmpty()) {
            throw new IllegalArgumentException("No data provided for insert");
        }

        List<String> columns = data.keySet().stream()
                .map(col -> SqlEscapeUtil.escapeColumnName(col, dbType))
                .collect(Collectors.toList());

        List<String> parameterNames = data.keySet().stream()
                .map(col -> ":" + col)
                .collect(Collectors.toList());

        String query = String.format(
                "INSERT INTO %s (%s) VALUES (%s)",
                fullTableName,
                String.join(", ", columns),
                String.join(", ", parameterNames)
        );

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        data.forEach(parameterSource::addValue);

        return new InsertQueryResult(query, data, parameterSource);
    }

    /**
     * Get created record data including generated keys
     */
    private Map<String, Object> getCreatedRecordData(
            Map<String, Object> originalData, KeyHolder keyHolder, List<ColumnMetadata> columns
    ) {
        Map<String, Object> createdRecord = new HashMap<>(originalData);

        // Add generated keys for auto-increment columns
        if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            keyHolder.getKeys().forEach((key, value) -> {
                // Find corresponding auto-increment column
                columns.stream()
                        .filter(ColumnMetadata::autoIncrement)
                        .filter(col -> col.columnName().equalsIgnoreCase(key))
                        .findFirst()
                        .ifPresent(col -> createdRecord.put(col.columnName(), value));
            });
        }

        return createdRecord;
    }

    /**
     * Build full table name with schema
     */
    private String buildTableName(String schemaName, String tableName, DatabaseType dbType) {
        if (schemaName != null && !schemaName.isEmpty()) {
            return SqlEscapeUtil.escapeSchemaName(schemaName, dbType) + "." + SqlEscapeUtil.escapeTableName(tableName, dbType);
        }
        return SqlEscapeUtil.escapeTableName(tableName, dbType);
    }


    /**
     * Result containing generated INSERT query and parameters
     */
    private record InsertQueryResult(
            String query,
            Map<String, Object> parameters,
            MapSqlParameterSource parameterSource
    ) {
    }
}

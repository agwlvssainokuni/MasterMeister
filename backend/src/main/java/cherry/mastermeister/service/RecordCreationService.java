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

import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.RecordCreationResult;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.util.PermissionUtils;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecordCreationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseConnectionService databaseConnectionService;
    private final SchemaMetadataStorageService schemaMetadataStorageService;
    private final PermissionUtils permissionUtils;
    private final AuditLogService auditLogService;

    public RecordCreationService(
            DatabaseConnectionService databaseConnectionService,
            SchemaMetadataStorageService schemaMetadataStorageService,
            PermissionUtils permissionUtils,
            AuditLogService auditLogService
    ) {
        this.databaseConnectionService = databaseConnectionService;
        this.schemaMetadataStorageService = schemaMetadataStorageService;
        this.permissionUtils = permissionUtils;
        this.auditLogService = auditLogService;
    }

    /**
     * Create new record with permission validation
     */
    public RecordCreationResult createRecord(
            Long connectionId, String schemaName, String tableName, Map<String, Object> recordData
    ) {
        logger.info("Creating record in table {}.{} on connection: {}", schemaName, tableName, connectionId);
        
        long startTime = System.currentTimeMillis();

        // Check WRITE permission for the table
        permissionUtils.requireTablePermission(connectionId, PermissionType.WRITE, schemaName, tableName);

        try {
            // Get table metadata
            TableMetadata tableMetadata = schemaMetadataStorageService.getTableMetadata(
                    connectionId, schemaName, tableName);

            // Get writable columns with permissions
            List<ColumnMetadata> writableColumns = getWritableColumns(connectionId, tableMetadata);

            if (writableColumns.isEmpty()) {
                throw new IllegalArgumentException("No writable columns found for table " + schemaName + "." + tableName);
            }

            // Validate and filter input data
            Map<String, Object> validatedData = validateAndFilterData(recordData, writableColumns);

            // Build and execute INSERT query
            DataSource dataSource = databaseConnectionService.getDataSource(connectionId);
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            InsertQueryResult insertQuery = buildInsertQuery(schemaName, tableName, validatedData);
            
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

            // Get the created record with generated keys
            Map<String, Object> createdRecord = getCreatedRecordData(validatedData, keyHolder, writableColumns);
            Map<String, String> columnTypes = writableColumns.stream()
                    .collect(Collectors.toMap(ColumnMetadata::columnName, ColumnMetadata::dataType));

            long executionTime = System.currentTimeMillis() - startTime;

            RecordCreationResult result = new RecordCreationResult(
                    createdRecord, columnTypes, executionTime, insertQuery.query());

            // Log record creation
            auditLogService.logDataModification(connectionId, schemaName, tableName, 
                    "CREATE", 1, executionTime);

            logger.info("Successfully created record in {}.{} in {}ms", schemaName, tableName, executionTime);

            return result;

        } catch (DataAccessException e) {
            logger.error("Failed to create record in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to create record", e);
        }
    }

    /**
     * Get writable columns based on user permissions
     */
    private List<ColumnMetadata> getWritableColumns(
            Long connectionId, TableMetadata tableMetadata
    ) {
        return tableMetadata.columns().stream()
                .filter(column -> permissionUtils.hasColumnPermission(
                        connectionId, PermissionType.WRITE,
                        tableMetadata.schema(), tableMetadata.tableName(), column.columnName()))
                .collect(Collectors.toList());
    }

    /**
     * Validate and filter input data based on column permissions and metadata
     */
    private Map<String, Object> validateAndFilterData(
            Map<String, Object> inputData, List<ColumnMetadata> writableColumns
    ) {
        Map<String, Object> validatedData = new HashMap<>();

        for (ColumnMetadata column : writableColumns) {
            String columnName = column.columnName();
            
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
     * Build INSERT query with parameterized values
     */
    private InsertQueryResult buildInsertQuery(
            String schemaName, String tableName, Map<String, Object> data
    ) {
        String fullTableName = buildTableName(schemaName, tableName);
        
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No data provided for insert");
        }

        List<String> columns = data.keySet().stream()
                .map(this::escapeColumnName)
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
    private String buildTableName(String schemaName, String tableName) {
        if (schemaName != null && !schemaName.isEmpty()) {
            return escapeTableName(schemaName) + "." + escapeTableName(tableName);
        }
        return escapeTableName(tableName);
    }

    /**
     * Escape table name for SQL (basic implementation)
     */
    private String escapeTableName(String tableName) {
        return "\"" + tableName.replace("\"", "\"\"") + "\"";
    }

    /**
     * Escape column name for SQL (basic implementation)
     */
    private String escapeColumnName(String columnName) {
        return "\"" + columnName.replace("\"", "\"\"") + "\"";
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
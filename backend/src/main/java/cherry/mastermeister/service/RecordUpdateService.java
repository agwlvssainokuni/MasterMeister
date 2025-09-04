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
import cherry.mastermeister.model.RecordUpdateResult;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.util.SqlEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecordUpdateService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseService databaseService;
    private final SchemaMetadataService schemaMetadataService;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    public RecordUpdateService(
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
     * Update records with permission validation and transaction management
     */
    public RecordUpdateResult updateRecord(
            Long connectionId, String schemaName, String tableName,
            Map<String, Object> updateData, Map<String, Object> whereConditions
    ) {
        logger.info("Updating records in table {}.{} on connection: {}", schemaName, tableName, connectionId);

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
                    connectionId, schemaName, tableName
            );

            // Filter update data to only include writable columns
            Map<String, Object> validatedUpdateData = filterWritableUpdateData(updateData, writableColumnNames, tableMetadata);

            // Get readable columns for WHERE conditions validation
            List<String> readableColumns = permissionService.getReadableColumns(connectionId, schemaName, tableName);
            Map<String, Object> validatedWhereConditions = validateWhereConditions(whereConditions, readableColumns);

            if (validatedUpdateData.isEmpty()) {
                throw new IllegalArgumentException("No valid columns to update");
            }

            if (validatedWhereConditions.isEmpty()) {
                throw new IllegalArgumentException("WHERE conditions are required for safety");
            }

            // Build and execute UPDATE query
            DataSource dataSource = databaseService.getDataSource(connectionId);
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            // Get database type for proper SQL escaping
            DatabaseConnection dbConnection = databaseService.getConnection(connectionId);
            DatabaseType dbType = dbConnection.dbType();

            UpdateQueryResult updateQuery = buildUpdateQuery(schemaName, tableName,
                    validatedUpdateData, validatedWhereConditions, dbType);

            logger.debug("Executing UPDATE query: {}", updateQuery.query());
            logger.debug("Query parameters: {}", updateQuery.parameters());

            int rowsAffected = namedJdbcTemplate.update(updateQuery.query(), updateQuery.parameterSource());

            long executionTime = System.currentTimeMillis() - startTime;

            RecordUpdateResult result = new RecordUpdateResult(
                    rowsAffected, executionTime, updateQuery.query());

            // Log detailed record update success
            auditLogService.logDataModificationDetailed(connectionId, schemaName, tableName,
                    "UPDATE", rowsAffected, executionTime, updateQuery.query(),
                    String.format("Updated %d records, SET %d fields, WHERE %d conditions",
                            rowsAffected, updateData.size(), whereConditions.size()));

            logger.info("Successfully updated {} records in {}.{} in {}ms",
                    rowsAffected, schemaName, tableName, executionTime);

            return result;

        } catch (DataAccessException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log detailed database failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "UPDATE", e.getMessage(), executionTime,
                    String.format("UPDATE failed: SET %d fields, WHERE %d conditions",
                            updateData.size(), whereConditions.size()));

            logger.error("Failed to update records in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to update records", e);
        } catch (IllegalArgumentException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log validation failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "UPDATE", "Validation failed: " + e.getMessage(), executionTime,
                    "Data validation, permission check, or WHERE clause validation failed");

            logger.error("Validation error updating records in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log unexpected failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "UPDATE", "Unexpected error: " + e.getMessage(), executionTime,
                    "Internal server error during record update");

            logger.error("Unexpected error updating records in table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to update records due to internal error", e);
        }
    }

    /**
     * Filter update data to only include writable columns
     */
    private Map<String, Object> filterWritableUpdateData(
            Map<String, Object> updateData, List<String> writableColumnNames, TableMetadata tableMetadata
    ) {
        Map<String, Object> validatedData = new HashMap<>();

        for (Map.Entry<String, Object> entry : updateData.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            // Check if column is writable
            if (!writableColumnNames.contains(columnName)) {
                logger.warn("Column '{}' is not writable, skipping update", columnName);
                continue;
            }

            // Find column metadata
            ColumnMetadata column = tableMetadata.columns().stream()
                    .filter(col -> col.columnName().equals(columnName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnName));

            // Skip auto-increment columns - they should not be updated manually
            if (column.autoIncrement()) {
                logger.warn("Column '{}' is auto-increment, skipping update", columnName);
                continue;
            }

            // Basic validation - null check for NOT NULL columns
            if (value == null && !column.nullable()) {
                throw new IllegalArgumentException(
                        "Column '" + columnName + "' cannot be null (NOT NULL constraint)");
            }

            validatedData.put(columnName, value);
        }

        return validatedData;
    }

    /**
     * Validate WHERE conditions based on column permissions
     */
    private Map<String, Object> validateWhereConditions(
            Map<String, Object> whereConditions, List<String> readableColumns
    ) {
        Map<String, Object> validatedConditions = new HashMap<>();

        for (Map.Entry<String, Object> entry : whereConditions.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            // Check if column is readable
            boolean isReadable = readableColumns.stream().anyMatch(columnName::equals);

            if (!isReadable) {
                throw new IllegalArgumentException(
                        "Column '" + columnName + "' is not readable for WHERE conditions");
            }

            validatedConditions.put(columnName, value);
        }

        return validatedConditions;
    }

    /**
     * Build UPDATE query with parameterized values
     */
    private UpdateQueryResult buildUpdateQuery(
            String schemaName, String tableName,
            Map<String, Object> updateData, Map<String, Object> whereConditions, DatabaseType dbType
    ) {
        String fullTableName = buildTableName(schemaName, tableName, dbType);

        // Build SET clause
        List<String> setClause = updateData.keySet().stream()
                .map(col -> SqlEscapeUtil.escapeColumnName(col, dbType) + " = :update_" + col)
                .collect(Collectors.toList());

        // Build WHERE clause
        List<String> whereClause = whereConditions.keySet().stream()
                .map(col -> SqlEscapeUtil.escapeColumnName(col, dbType) + " = :where_" + col)
                .collect(Collectors.toList());

        String query = String.format(
                "UPDATE %s SET %s WHERE %s",
                fullTableName,
                String.join(", ", setClause),
                String.join(" AND ", whereClause)
        );

        // Combine parameters with prefixes to avoid conflicts
        Map<String, Object> allParameters = new HashMap<>();
        updateData.forEach((key, value) -> allParameters.put("update_" + key, value));
        whereConditions.forEach((key, value) -> allParameters.put("where_" + key, value));

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        allParameters.forEach(parameterSource::addValue);

        return new UpdateQueryResult(query, allParameters, parameterSource);
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
     * Result containing generated UPDATE query and parameters
     */
    private record UpdateQueryResult(
            String query,
            Map<String, Object> parameters,
            MapSqlParameterSource parameterSource
    ) {
    }
}

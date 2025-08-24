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
import cherry.mastermeister.model.*;
import cherry.mastermeister.util.PermissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecordAccessService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseConnectionService databaseConnectionService;
    private final SchemaMetadataStorageService schemaMetadataStorageService;
    private final PermissionAuthService permissionAuthService;
    private final PermissionUtils permissionUtils;
    private final AuditLogService auditLogService;
    private final QueryBuilderService queryBuilderService;

    @Value("${mm.app.data-access.large-dataset-threshold:100}")
    private int largeDatasetThreshold;

    public RecordAccessService(
            DatabaseConnectionService databaseConnectionService,
            SchemaMetadataStorageService schemaMetadataStorageService,
            PermissionAuthService permissionAuthService,
            PermissionUtils permissionUtils,
            AuditLogService auditLogService,
            QueryBuilderService queryBuilderService
    ) {
        this.databaseConnectionService = databaseConnectionService;
        this.schemaMetadataStorageService = schemaMetadataStorageService;
        this.permissionAuthService = permissionAuthService;
        this.permissionUtils = permissionUtils;
        this.auditLogService = auditLogService;
        this.queryBuilderService = queryBuilderService;
    }

    /**
     * Get records from table with column-level permission filtering
     */
    public RecordQueryResult getRecords(
            Long connectionId, String schemaName, String tableName, int page, int pageSize
    ) {
        return getRecords(connectionId, schemaName, tableName, RecordFilter.empty(), page, pageSize);
    }

    /**
     * Get records from table with filtering and column-level permission filtering
     */
    public RecordQueryResult getRecords(
            Long connectionId, String schemaName, String tableName, RecordFilter filter, int page, int pageSize
    ) {
        logger.info("Getting records for table {}.{} on connection: {}, page: {}, size: {}",
                schemaName, tableName, connectionId, page, pageSize);

        long startTime = System.currentTimeMillis();

        // Check READ permission for the table
        permissionUtils.requireTablePermission(connectionId, PermissionType.READ, schemaName, tableName);

        try {
            // Get table metadata
            TableMetadata tableMetadata = schemaMetadataStorageService.getTableMetadata(
                    connectionId, schemaName, tableName);

            // Get accessible columns with permissions
            List<ColumnMetadata> accessibleColumns = getAccessibleColumns(connectionId, tableMetadata);

            if (accessibleColumns.isEmpty()) {
                logger.warn("No accessible columns found for table {}.{}", schemaName, tableName);
                return createEmptyResult(page, pageSize, System.currentTimeMillis() - startTime);
            }

            // Build and execute query
            DataSource dataSource = databaseConnectionService.getDataSource(connectionId);
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            // Use QueryBuilderService for dynamic query generation
            QueryBuilderService.QueryResult selectQueryResult = queryBuilderService.buildSelectQuery(
                    schemaName, tableName, accessibleColumns, filter, page, pageSize);
            QueryBuilderService.QueryResult countQueryResult = queryBuilderService.buildCountQuery(
                    schemaName, tableName, filter);

            logger.debug("Executing query: {}", selectQueryResult.query());
            logger.debug("Query parameters: {}", selectQueryResult.parameters());

            // Get total count
            Long totalRecords = namedJdbcTemplate.queryForObject(
                    countQueryResult.query(), countQueryResult.parameters(), Long.class);
            if (totalRecords == null) {
                totalRecords = 0L;
            }

            // Execute main query
            List<TableRecord> records = namedJdbcTemplate.query(
                    selectQueryResult.query(), selectQueryResult.parameters(),
                    (rs, rowNum) -> mapResultSetToRecord(rs, accessibleColumns, connectionId, schemaName, tableName));

            long executionTime = System.currentTimeMillis() - startTime;

            RecordQueryResult result = new RecordQueryResult(
                    records, accessibleColumns, totalRecords, page, pageSize, executionTime, selectQueryResult.query());

            // Log large dataset access
            if (result.isLargeDataset(largeDatasetThreshold)) {
                auditLogService.logDataAccess(connectionId, schemaName, tableName, records.size(), executionTime);
            }

            logger.info("Retrieved {} records out of {} total for {}.{} in {}ms",
                    records.size(), totalRecords, schemaName, tableName, executionTime);

            return result;

        } catch (DataAccessException e) {
            logger.error("Failed to retrieve records for table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to retrieve table records", e);
        }
    }

    /**
     * Get accessible columns based on user permissions
     */
    private List<ColumnMetadata> getAccessibleColumns(
            Long connectionId, TableMetadata tableMetadata
    ) {
        return tableMetadata.columns().stream()
                .filter(column -> permissionUtils.hasColumnPermission(
                        connectionId, PermissionType.READ,
                        tableMetadata.schema(), tableMetadata.tableName(), column.columnName()))
                .collect(Collectors.toList());
    }

    /**
     * Build SELECT query with only accessible columns
     */
    private String buildSelectQuery(
            String schemaName, String tableName, List<ColumnMetadata> columns, int page, int pageSize
    ) {
        String columnList = columns.stream()
                .map(col -> escapeColumnName(col.columnName()))
                .collect(Collectors.joining(", "));

        String fullTableName = schemaName != null && !schemaName.isEmpty()
                ? escapeTableName(schemaName) + "." + escapeTableName(tableName)
                : escapeTableName(tableName);

        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(columnList).append(" FROM ").append(fullTableName);

        // Add pagination (basic implementation - may need database-specific optimization)
        int offset = page * pageSize;
        query.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        return query.toString();
    }

    /**
     * Build COUNT query for total records
     */
    private String buildCountQuery(String schemaName, String tableName) {
        String fullTableName = schemaName != null && !schemaName.isEmpty()
                ? escapeTableName(schemaName) + "." + escapeTableName(tableName)
                : escapeTableName(tableName);

        return "SELECT COUNT(*) FROM " + fullTableName;
    }

    /**
     * Map ResultSet to TableRecord with column permissions
     */
    private TableRecord mapResultSetToRecord(
            ResultSet rs, List<ColumnMetadata> accessibleColumns,
            Long connectionId, String schemaName, String tableName
    ) throws SQLException {

        ResultSetMetaData metaData = rs.getMetaData();
        Map<String, Object> data = new HashMap<>();
        Map<String, String> columnTypes = new HashMap<>();
        Map<String, Boolean> columnPermissions = new HashMap<>();

        // Process all columns in result set
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            String columnType = metaData.getColumnTypeName(i);
            Object value = rs.getObject(i);

            data.put(columnName, value);
            columnTypes.put(columnName, columnType);

            // Check if column is in accessible list
            boolean canRead = accessibleColumns.stream()
                    .anyMatch(col -> col.columnName().equals(columnName));
            columnPermissions.put(columnName, canRead);
        }

        return new TableRecord(data, columnTypes, columnPermissions);
    }

    /**
     * Create empty result for cases with no data or no permissions
     */
    private RecordQueryResult createEmptyResult(int page, int pageSize, long executionTime) {
        return new RecordQueryResult(
                List.of(), List.of(), 0L, page, pageSize, executionTime, "-- No accessible columns --");
    }

    /**
     * Escape table name for SQL (basic implementation)
     */
    private String escapeTableName(String tableName) {
        // Basic escaping - may need database-specific implementation
        return "\"" + tableName.replace("\"", "\"\"") + "\"";
    }

    /**
     * Escape column name for SQL (basic implementation)
     */
    private String escapeColumnName(String columnName) {
        // Basic escaping - may need database-specific implementation
        return "\"" + columnName.replace("\"", "\"\"") + "\"";
    }
}

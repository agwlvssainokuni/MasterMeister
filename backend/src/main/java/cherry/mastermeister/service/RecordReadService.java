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

import cherry.mastermeister.entity.ColumnMetadataEntity;
import cherry.mastermeister.entity.SchemaMetadataEntity;
import cherry.mastermeister.entity.TableMetadataEntity;
import cherry.mastermeister.enums.DatabaseType;
import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.exception.DatabaseNotFoundException;
import cherry.mastermeister.exception.PermissionDeniedException;
import cherry.mastermeister.exception.TableNotFoundException;
import cherry.mastermeister.model.*;
import cherry.mastermeister.repository.SchemaMetadataRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecordReadService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseService databaseService;
    private final SchemaMetadataRepository schemaMetadataRepository;
    private final PermissionService permissionService;
    private final QueryBuilderService queryBuilderService;
    private final AuditLogService auditLogService;
    private final int largeDatasetThreshold;

    public RecordReadService(
            DatabaseService databaseService,
            SchemaMetadataRepository schemaMetadataRepository,
            PermissionService permissionService,
            QueryBuilderService queryBuilderService,
            AuditLogService auditLogService,
            @Value("${mm.app.data-access.large-dataset-threshold:100}") int largeDatasetThreshold
    ) {
        this.databaseService = databaseService;
        this.schemaMetadataRepository = schemaMetadataRepository;
        this.permissionService = permissionService;
        this.queryBuilderService = queryBuilderService;
        this.auditLogService = auditLogService;
        this.largeDatasetThreshold = largeDatasetThreshold;
    }

    /**
     * Get records from table with filtering and column-level permission filtering
     */
    public RecordQueryResult getRecords(
            Long connectionId,
            String schemaName, String tableName,
            RecordFilter filter,
            int page, int pageSize
    ) {
        logger.info("Getting records for table {}.{} on connection: {}, page: {}, size: {}",
                schemaName, tableName, connectionId, page, pageSize);

        long startTime = System.currentTimeMillis();

        // Check READ permission for the table
        if (!permissionService.hasReadPermission(connectionId, schemaName, tableName)) {
            throw new PermissionDeniedException("READ permission required for table " + schemaName + "." + tableName, null);
        }

        try {
            // Get table entity directly from repository
            TableMetadataEntity tableEntity = getTableEntity(connectionId, schemaName, tableName);

            // Get accessible columns with permissions
            List<AccessibleColumn> accessibleColumns = getAccessibleColumns(connectionId, tableEntity);

            if (accessibleColumns.isEmpty()) {
                logger.warn("No accessible columns found for table {}.{}", schemaName, tableName);
                return createEmptyResult(page, pageSize, System.currentTimeMillis() - startTime);
            }

            // Build and execute query
            DataSource dataSource = databaseService.getDataSource(connectionId);
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            // Get database type for proper SQL escaping
            DatabaseConnection dbConnection = databaseService.getConnection(connectionId);
            DatabaseType dbType = dbConnection.dbType();

            // Use QueryBuilderService for dynamic query generation
            QueryBuilderService.QueryResult selectQueryResult = queryBuilderService.buildSelectQuery(
                    schemaName, tableName, accessibleColumns, filter, page, pageSize, dbType);
            QueryBuilderService.QueryResult countQueryResult = queryBuilderService.buildCountQuery(
                    schemaName, tableName, filter, dbType);

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
     * Get table entity from repository
     */
    private TableMetadataEntity getTableEntity(
            Long connectionId,
            String schemaName, String tableName
    ) {
        SchemaMetadataEntity schemaEntity = schemaMetadataRepository.findByConnectionId(connectionId)
                .orElseThrow(() -> new DatabaseNotFoundException("Schema metadata not found for connection: " + connectionId));

        return schemaEntity.getTables().stream()
                .filter(table -> schemaName.equals(table.getSchema()) && tableName.equals(table.getTableName()))
                .findFirst()
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + schemaName + "." + tableName));
    }

    /**
     * Get accessible columns with permission information
     */
    private List<AccessibleColumn> getAccessibleColumns(
            Long connectionId,
            TableMetadataEntity tableEntity
    ) {
        // Get all column names for bulk permission check
        List<String> allColumnNames = tableEntity.getColumns().stream()
                .map(ColumnMetadataEntity::getColumnName)
                .collect(Collectors.toList());

        // Get all column permissions in bulk (optimized - single query)
        Map<String, Set<PermissionType>> bulkColumnPermissions = permissionService.getBulkColumnPermissions(
                connectionId, tableEntity.getSchema(), tableEntity.getTableName(), allColumnNames
        );

        return tableEntity.getColumns().stream()
                .filter(columnEntity -> {
                    Set<PermissionType> permissions = bulkColumnPermissions.get(columnEntity.getColumnName());
                    return permissions != null && permissions.contains(PermissionType.READ);
                })
                .map(columnEntity -> {
                    Set<PermissionType> columnPermissions = bulkColumnPermissions.get(columnEntity.getColumnName());

                    return new AccessibleColumn(
                            columnEntity.getColumnName(),
                            columnEntity.getDataType(),
                            columnEntity.getColumnSize(),
                            columnEntity.getDecimalDigits(),
                            columnEntity.getNullable(),
                            columnEntity.getDefaultValue(),
                            columnEntity.getComment(),
                            columnEntity.getPrimaryKey(),
                            columnEntity.getAutoIncrement(),
                            columnEntity.getOrdinalPosition(),
                            columnPermissions,
                            columnPermissions.contains(PermissionType.READ),
                            columnPermissions.contains(PermissionType.WRITE),
                            columnPermissions.contains(PermissionType.DELETE),
                            columnPermissions.contains(PermissionType.ADMIN)
                    );
                })
                .collect(Collectors.toList());
    }


    /**
     * Map ResultSet to TableRecord with column permissions
     */
    private TableRecord mapResultSetToRecord(
            ResultSet rs, List<AccessibleColumn> accessibleColumns,
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
                List.of(), List.of(),
                0L, page, pageSize,
                executionTime,
                "-- No accessible columns --"
        );
    }
}

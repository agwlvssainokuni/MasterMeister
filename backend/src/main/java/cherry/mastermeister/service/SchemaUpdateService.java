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

import cherry.mastermeister.entity.SchemaUpdateLogEntity;
import cherry.mastermeister.enums.DatabaseType;
import cherry.mastermeister.enums.SchemaUpdateOperation;
import cherry.mastermeister.model.*;
import cherry.mastermeister.repository.SchemaUpdateLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

@Service
public class SchemaUpdateService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseService databaseService;
    private final SchemaMetadataService schemaMetadataService;
    private final SchemaUpdateLogRepository schemaUpdateLogRepository;
    private final AuditLogService auditLogService;

    public SchemaUpdateService(
            DatabaseService databaseService,
            SchemaMetadataService schemaMetadataService,
            SchemaUpdateLogRepository schemaUpdateLogRepository,
            AuditLogService auditLogService
    ) {
        this.databaseService = databaseService;
        this.schemaMetadataService = schemaMetadataService;
        this.schemaUpdateLogRepository = schemaUpdateLogRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SchemaMetadata executeSchemaRead(Long connectionId) {
        return executeWithLogging(connectionId, SchemaUpdateOperation.READ_SCHEMA,
                () -> readAndRefreshSchema(connectionId));
    }

    @Transactional
    public SchemaMetadata executeSchemaRefresh(Long connectionId) {
        return executeWithLogging(connectionId, SchemaUpdateOperation.REFRESH_SCHEMA,
                () -> readAndRefreshSchema(connectionId));
    }

    @Transactional
    public Optional<SchemaMetadata> getStoredSchema(Long connectionId) {
        // This doesn't need logging as it's just a read operation
        return getStoredSchemaMetadata(connectionId);
    }

    @Transactional(readOnly = true)
    public List<SchemaUpdateLog> getConnectionOperationHistory(Long connectionId) {
        logger.debug("Retrieving operation history for connection ID: {}", connectionId);

        return schemaUpdateLogRepository.findByConnectionIdOrderByCreatedAtDesc(connectionId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SchemaUpdateLog> getConnectionOperationHistory(Long connectionId, Pageable pageable) {
        logger.debug("Retrieving paginated operation history for connection ID: {}", connectionId);

        return schemaUpdateLogRepository.findByConnectionIdOrderByCreatedAtDesc(connectionId, pageable)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<SchemaUpdateLog> getFailedOperations(Long connectionId) {
        logger.debug("Retrieving failed operations for connection ID: {}", connectionId);

        return schemaUpdateLogRepository.findFailedOperationsByConnection(connectionId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getSuccessfulOperationsCount(Long connectionId) {
        return schemaUpdateLogRepository.countSuccessfulOperationsByConnection(connectionId);
    }

    @Transactional(readOnly = true)
    public Page<SchemaUpdateLog> getUserOperationHistory(String userEmail, Pageable pageable) {
        logger.debug("Retrieving operation history for user: {}", userEmail);

        return schemaUpdateLogRepository.findByUserEmailOrderByCreatedAtDesc(userEmail, pageable)
                .map(this::toModel);
    }

    private <T> T executeWithLogging(Long connectionId, SchemaUpdateOperation operation, Supplier<T> schemaOperation) {
        String userEmail = getCurrentUserEmail();
        LocalDateTime startTime = LocalDateTime.now();
        long startTimeMs = System.currentTimeMillis();

        SchemaUpdateLogEntity logEntity = new SchemaUpdateLogEntity();
        logEntity.setConnectionId(connectionId);
        logEntity.setOperation(operation);
        logEntity.setUserEmail(userEmail);
        logEntity.setCreatedAt(startTime);

        try {
            logger.info("Executing {} for connection ID: {} by user: {}", operation, connectionId, userEmail);

            T result = schemaOperation.get();
            long executionTime = System.currentTimeMillis() - startTimeMs;

            // Log success
            logEntity.setExecutionTimeMs(executionTime);
            logEntity.setSuccess(true);

            if (result instanceof SchemaMetadata metadata) {
                logEntity.setTablesCount(metadata.tables().size());
                logEntity.setColumnsCount(metadata.tables().stream()
                        .mapToInt(table -> table.columns().size())
                        .sum());
                logEntity.setDetails(String.format("Successfully processed %d schemas, %d tables, %d columns",
                        metadata.schemas().size(), metadata.tables().size(),
                        metadata.tables().stream().mapToInt(table -> table.columns().size()).sum()));
            }

            schemaUpdateLogRepository.save(logEntity);
            logger.info("Successfully executed {} for connection ID: {} in {}ms",
                    operation, connectionId, executionTime);

            // Log admin action
            auditLogService.logSchemaOperation(userEmail, operation.name(), connectionId, true,
                    logEntity.getDetails(), null);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTimeMs;

            // Log failure
            logEntity.setExecutionTimeMs(executionTime);
            logEntity.setSuccess(false);
            logEntity.setErrorMessage(e.getMessage());
            logEntity.setDetails("Operation failed: " + e.getClass().getSimpleName());

            schemaUpdateLogRepository.save(logEntity);
            logger.error("Failed to execute {} for connection ID: {} in {}ms",
                    operation, connectionId, executionTime, e);

            // Log admin action failure
            auditLogService.logSchemaOperation(userEmail, operation.name(), connectionId, false,
                    logEntity.getDetails(), e.getMessage());

            throw e;
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "system";
    }

    // Schema reading methods integrated from SchemaReaderService
    public SchemaMetadata readAndRefreshSchema(Long connectionId) {
        logger.info("Reading and refreshing schema metadata for connection ID: {}", connectionId);

        DatabaseConnection connection = databaseService.getConnection(connectionId);
        DataSource dataSource = databaseService.getDataSource(connectionId);

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            List<String> schemas = readSchemas(metaData, connection.dbType());
            List<TableMetadata> tables = readTables(metaData, connection, schemas);

            SchemaMetadata schemaMetadata = new SchemaMetadata(
                    connectionId,
                    connection.databaseName(),
                    schemas,
                    tables,
                    LocalDateTime.now()
            );

            // Save the schema metadata to the database
            return schemaMetadataService.saveSchemaMetadata(schemaMetadata);
        } catch (SQLException e) {
            logger.error("Failed to read schema metadata for connection ID: {}", connectionId, e);
            throw new RuntimeException("Schema reading failed", e);
        }
    }

    public Optional<SchemaMetadata> getStoredSchemaMetadata(Long connectionId) {
        logger.debug("Retrieving stored schema metadata for connection ID: {}", connectionId);
        return schemaMetadataService.getSchemaMetadata(connectionId);
    }

    private List<String> readSchemas(DatabaseMetaData metaData, DatabaseType dbType) throws SQLException {
        List<String> schemas = new ArrayList<>();

        switch (dbType) {
            case MYSQL, MARIADB -> {
                // MySQL/MariaDB: schemas are databases
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_CAT");
                        if (!isSystemSchema(schema, dbType)) {
                            schemas.add(schema);
                        }
                    }
                }
            }
            case POSTGRESQL -> {
                // PostgreSQL: use schemas within database
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_SCHEM");
                        if (!isSystemSchema(schema, dbType)) {
                            schemas.add(schema);
                        }
                    }
                }
            }
            case H2 -> {
                // H2: use schemas
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_SCHEM");
                        if (!isSystemSchema(schema, dbType)) {
                            schemas.add(schema);
                        }
                    }
                }
            }
        }

        return schemas;
    }

    private List<TableMetadata> readTables(DatabaseMetaData metaData, DatabaseConnection connection, List<String> schemas) throws SQLException {
        List<TableMetadata> tables = new ArrayList<>();
        String[] tableTypes = {"TABLE", "VIEW"};

        for (String schema : schemas) {
            String catalog = getCatalogForSchema(connection.dbType(), schema, connection.databaseName());
            String schemaParam = getSchemaParam(connection.dbType(), schema);

            try (ResultSet rs = metaData.getTables(catalog, schemaParam, null, tableTypes)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableType = rs.getString("TABLE_TYPE");
                    String comment = rs.getString("REMARKS");

                    List<ColumnMetadata> columns = readColumns(metaData, catalog, schemaParam, tableName, connection.dbType());

                    tables.add(new TableMetadata(schema, tableName, tableType, comment, columns));
                }
            }
        }

        return tables;
    }

    private List<ColumnMetadata> readColumns(DatabaseMetaData metaData, String catalog, String schema,
                                             String tableName, DatabaseType dbType) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();
        Set<String> primaryKeys = readPrimaryKeys(metaData, catalog, schema, tableName);

        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                Integer columnSize = rs.getInt("COLUMN_SIZE");
                if (rs.wasNull()) columnSize = null;

                Integer decimalDigits = rs.getInt("DECIMAL_DIGITS");
                if (rs.wasNull()) decimalDigits = null;

                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                String defaultValue = rs.getString("COLUMN_DEF");
                String comment = rs.getString("REMARKS");
                boolean isPrimaryKey = primaryKeys.contains(columnName);
                boolean autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                int ordinalPosition = rs.getInt("ORDINAL_POSITION");

                columns.add(new ColumnMetadata(
                        columnName, dataType, columnSize, decimalDigits, nullable,
                        defaultValue, comment, isPrimaryKey, autoIncrement, ordinalPosition
                ));
            }
        }

        return columns.stream()
                .sorted(Comparator.comparing(ColumnMetadata::ordinalPosition))
                .toList();
    }

    private Set<String> readPrimaryKeys(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();

        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        return primaryKeys;
    }

    private boolean isSystemSchema(String schema, DatabaseType dbType) {
        if (schema == null) return true;

        return switch (dbType) {
            case MYSQL, MARIADB -> Set.of(
                    "information_schema", "performance_schema", "mysql", "sys"
            ).contains(schema.toLowerCase());

            case POSTGRESQL -> Set.of(
                    "information_schema", "pg_catalog", "pg_toast"
            ).contains(schema.toLowerCase()) || schema.toLowerCase().startsWith("pg_");

            case H2 -> Set.of(
                    "INFORMATION_SCHEMA"
            ).contains(schema.toUpperCase());
        };
    }

    private String getCatalogForSchema(DatabaseType dbType, String schema, String databaseName) {
        return switch (dbType) {
            case MYSQL, MARIADB -> schema; // schema IS catalog in MySQL/MariaDB
            case POSTGRESQL, H2 -> databaseName; // use database name as catalog
        };
    }

    private String getSchemaParam(DatabaseType dbType, String schema) {
        return switch (dbType) {
            case MYSQL, MARIADB -> null; // don't use schema parameter for MySQL/MariaDB
            case POSTGRESQL, H2 -> schema; // use schema parameter
        };
    }

    private SchemaUpdateLog toModel(SchemaUpdateLogEntity entity) {
        return new SchemaUpdateLog(
                entity.getId(),
                entity.getConnectionId(),
                entity.getOperation(),
                entity.getUserEmail(),
                entity.getExecutionTimeMs(),
                entity.getSuccess(),
                entity.getErrorMessage(),
                entity.getTablesCount(),
                entity.getColumnsCount(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }
}

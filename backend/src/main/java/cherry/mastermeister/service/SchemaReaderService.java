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
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SchemaReaderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseConnectionService databaseConnectionService;

    public SchemaReaderService(DatabaseConnectionService databaseConnectionService) {
        this.databaseConnectionService = databaseConnectionService;
    }

    public SchemaMetadata readSchema(Long connectionId) {
        DatabaseConnection connection = databaseConnectionService.getConnection(connectionId);
        DataSource dataSource = databaseConnectionService.getDataSource(connectionId);

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            List<String> schemas = readSchemas(metaData, connection.dbType());
            List<TableMetadata> tables = readTables(metaData, connection, schemas);

            return new SchemaMetadata(
                    connectionId,
                    connection.databaseName(),
                    schemas,
                    tables,
                    LocalDateTime.now()
            );
        } catch (SQLException e) {
            logger.error("Failed to read schema metadata for connection ID: {}", connectionId, e);
            throw new RuntimeException("Schema reading failed", e);
        }
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
}

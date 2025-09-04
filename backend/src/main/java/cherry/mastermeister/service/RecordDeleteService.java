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
import cherry.mastermeister.model.DatabaseConnection;
import cherry.mastermeister.model.RecordDeleteResult;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.util.SqlEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecordDeleteService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseService databaseService;
    private final SchemaMetadataService schemaMetadataService;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    public RecordDeleteService(
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
     * Delete records with permission validation and referential integrity checks
     */
    public RecordDeleteResult deleteRecord(
            Long connectionId, String schemaName, String tableName,
            Map<String, Object> whereConditions, boolean skipReferentialIntegrityCheck
    ) {
        logger.info("Deleting records from table {}.{} on connection: {}", schemaName, tableName, connectionId);

        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        // Check DELETE permission for ALL columns in the table
        if (!permissionService.hasDeletePermission(connectionId, schemaName, tableName)) {
            throw new IllegalArgumentException(
                    "DELETE permission required for ALL columns in table " + schemaName + "." + tableName);
        }

        try {
            // Get table metadata
            TableMetadata tableMetadata = schemaMetadataService.getTableMetadata(
                    connectionId, schemaName, tableName);

            // Get readable columns for WHERE conditions (need READ permission for WHERE)
            List<String> readableColumns = permissionService.getReadableColumns(connectionId, schemaName, tableName);

            // Validate WHERE conditions
            Map<String, Object> validatedWhereConditions = validateWhereConditions(whereConditions, readableColumns);

            if (validatedWhereConditions.isEmpty()) {
                throw new IllegalArgumentException("WHERE conditions are required for safe deletion");
            }

            // Get data source for operations
            DataSource dataSource = databaseService.getDataSource(connectionId);
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            // Perform referential integrity check if not skipped
            boolean integrityChecked = false;
            if (!skipReferentialIntegrityCheck) {
                List<String> integrityWarnings = checkReferentialIntegrity(
                        connectionId, dataSource, schemaName, tableName, validatedWhereConditions);
                warnings.addAll(integrityWarnings);
                integrityChecked = true;

                // If there are referential integrity violations, prevent deletion
                if (!integrityWarnings.isEmpty()) {
                    throw new DataIntegrityViolationException(
                            "Cannot delete records due to referential integrity constraints: " +
                                    String.join("; ", integrityWarnings));
                }
            }

            // Get database type for proper SQL escaping
            DatabaseConnection dbConnection = databaseService.getConnection(connectionId);
            DatabaseType dbType = dbConnection.dbType();

            // Build and execute DELETE query
            DeleteQueryResult deleteQuery = buildDeleteQuery(schemaName, tableName, validatedWhereConditions, dbType);

            logger.debug("Executing DELETE query: {}", deleteQuery.query());
            logger.debug("Query parameters: {}", deleteQuery.parameters());

            int rowsAffected = namedJdbcTemplate.update(deleteQuery.query(), deleteQuery.parameterSource());

            long executionTime = System.currentTimeMillis() - startTime;

            RecordDeleteResult result = new RecordDeleteResult(
                    rowsAffected, executionTime, deleteQuery.query(), integrityChecked, warnings);

            // Log detailed record deletion success
            String details = String.format("Deleted %d records, WHERE %d conditions, integrity check: %s, warnings: %d",
                    rowsAffected, whereConditions.size(), integrityChecked ? "performed" : "skipped", warnings.size());
            auditLogService.logDataModificationDetailed(connectionId, schemaName, tableName,
                    "DELETE", rowsAffected, executionTime, deleteQuery.query(), details);

            logger.info("Successfully deleted {} records from {}.{} in {}ms",
                    rowsAffected, schemaName, tableName, executionTime);

            return result;

        } catch (DataIntegrityViolationException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log referential integrity violation (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "DELETE", "Referential integrity violation: " + e.getMessage(), executionTime,
                    String.format("DELETE failed due to foreign key constraints, WHERE %d conditions", whereConditions.size()));

            logger.warn("Referential integrity violation prevented deletion from {}.{}: {}",
                    schemaName, tableName, e.getMessage());
            throw e;  // Re-throw to be handled by GlobalExceptionHandler
        } catch (DataAccessException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log database failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "DELETE", e.getMessage(), executionTime,
                    String.format("DELETE failed: WHERE %d conditions", whereConditions.size()));

            logger.error("Failed to delete records from table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to delete records", e);
        } catch (IllegalArgumentException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log validation failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "DELETE", "Validation failed: " + e.getMessage(), executionTime,
                    "Permission check or WHERE clause validation failed");

            logger.error("Validation error deleting records from table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log unexpected failure (REQUIRES_NEW ensures this is recorded)
            auditLogService.logDataModificationFailure(connectionId, schemaName, tableName,
                    "DELETE", "Unexpected error: " + e.getMessage(), executionTime,
                    "Internal server error during record deletion");

            logger.error("Unexpected error deleting records from table {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("Failed to delete records due to internal error", e);
        }
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
     * Check referential integrity before deletion
     */
    private List<String> checkReferentialIntegrity(
            Long connectionId, DataSource dataSource, String schemaName, String tableName,
            Map<String, Object> whereConditions
    ) {
        List<String> warnings = new ArrayList<>();

        try {
            // Get foreign keys that reference this table
            List<ForeignKeyReference> incomingReferences = getIncomingForeignKeyReferences(
                    dataSource, schemaName, tableName);

            if (incomingReferences.isEmpty()) {
                return warnings; // No references, safe to delete
            }

            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            for (ForeignKeyReference reference : incomingReferences) {
                // Build query to check for referencing records
                String checkQuery = buildReferenceCheckQuery(
                        connectionId, reference, schemaName, tableName, whereConditions);

                MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                whereConditions.forEach(parameterSource::addValue);

                logger.debug("Checking reference integrity: {}", checkQuery);

                Integer referencingCount = namedJdbcTemplate.queryForObject(
                        checkQuery, parameterSource, Integer.class);

                if (referencingCount != null && referencingCount > 0) {
                    warnings.add(String.format(
                            "Table %s.%s has %d referencing records in %s.%s",
                            reference.referencingSchema(), reference.referencingTable(),
                            referencingCount, schemaName, tableName));
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to check referential integrity: {}", e.getMessage());
            warnings.add("Could not verify referential integrity: " + e.getMessage());
        }

        return warnings;
    }

    /**
     * Get foreign key references pointing to this table
     */
    private List<ForeignKeyReference> getIncomingForeignKeyReferences(
            DataSource dataSource, String schemaName, String tableName
    ) throws SQLException {
        List<ForeignKeyReference> references = new ArrayList<>();

        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Get imported keys (foreign keys referencing this table)
            try (ResultSet rs = metaData.getExportedKeys(
                    connection.getCatalog(), schemaName, tableName)) {

                while (rs.next()) {
                    ForeignKeyReference reference = new ForeignKeyReference(
                            rs.getString("FKTABLE_SCHEM"),
                            rs.getString("FKTABLE_NAME"),
                            rs.getString("FKCOLUMN_NAME"),
                            rs.getString("PKTABLE_SCHEM"),
                            rs.getString("PKTABLE_NAME"),
                            rs.getString("PKCOLUMN_NAME")
                    );
                    references.add(reference);
                }
            }
        }

        return references;
    }

    /**
     * Build query to check for referencing records
     */
    private String buildReferenceCheckQuery(
            Long connectionId, ForeignKeyReference reference, String targetSchema, String targetTable,
            Map<String, Object> whereConditions
    ) {
        // This is a simplified implementation
        // In a real system, you would need to handle multiple column foreign keys properly
        // Get database type for proper SQL escaping
        DatabaseConnection dbConnection = databaseService.getConnection(connectionId);
        DatabaseType dbType = dbConnection.dbType();

        String referencingTable = buildTableName(reference.referencingSchema(), reference.referencingTable(), dbType);
        String targetTableName = buildTableName(targetSchema, targetTable, dbType);

        StringBuilder query = new StringBuilder();
        query.append("SELECT COUNT(*) FROM ").append(referencingTable)
                .append(" WHERE ").append(SqlEscapeUtil.escapeColumnName(reference.referencingColumnName(), dbType))
                .append(" IN (SELECT ").append(SqlEscapeUtil.escapeColumnName(reference.referencedColumnName(), dbType))
                .append(" FROM ").append(targetTableName)
                .append(" WHERE ");

        // Add WHERE conditions from the deletion criteria
        List<String> conditions = whereConditions.keySet().stream()
                .map(col -> SqlEscapeUtil.escapeColumnName(col, dbType) + " = :" + col)
                .collect(Collectors.toList());

        query.append(String.join(" AND ", conditions)).append(")");

        return query.toString();
    }

    /**
     * Build DELETE query with parameterized values
     */
    private DeleteQueryResult buildDeleteQuery(
            String schemaName, String tableName, Map<String, Object> whereConditions, DatabaseType dbType
    ) {
        String fullTableName = buildTableName(schemaName, tableName, dbType);

        // Build WHERE clause
        List<String> whereClause = whereConditions.keySet().stream()
                .map(col -> SqlEscapeUtil.escapeColumnName(col, dbType) + " = :" + col)
                .collect(Collectors.toList());

        String query = String.format(
                "DELETE FROM %s WHERE %s",
                fullTableName,
                String.join(" AND ", whereClause)
        );

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        whereConditions.forEach(parameterSource::addValue);

        return new DeleteQueryResult(query, whereConditions, parameterSource);
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
     * Result containing generated DELETE query and parameters
     */
    private record DeleteQueryResult(
            String query,
            Map<String, Object> parameters,
            MapSqlParameterSource parameterSource
    ) {
    }

    /**
     * Foreign key reference information
     */
    private record ForeignKeyReference(
            String referencingSchema,
            String referencingTable,
            String referencingColumnName,
            String referencedSchema,
            String referencedTable,
            String referencedColumnName
    ) {
    }
}

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

package cherry.mastermeister.util;

import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.UserPermission;
import cherry.mastermeister.service.PermissionAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SqlPermissionFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionAuthService permissionAuthService;

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN|UPDATE|INSERT\\s+INTO|DELETE\\s+FROM)\\s+(?:([\\w.]+)\\.)?(\\w+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COLUMN_PATTERN = Pattern.compile(
            "(?i)\\b(?:SELECT|SET|WHERE|GROUP\\s+BY|ORDER\\s+BY|HAVING)\\s+.*?\\b(\\w+)(?=\\s*[,=<>]|\\s*$)",
            Pattern.CASE_INSENSITIVE
    );

    public SqlPermissionFilter(PermissionAuthService permissionAuthService) {
        this.permissionAuthService = permissionAuthService;
    }

    /**
     * Validate SQL query against user permissions
     */
    public SqlValidationResult validateSqlQuery(String sql, Long userId, Long connectionId) {
        logger.debug("Validating SQL query for user: {}, connection: {}", userId, connectionId);

        if (permissionAuthService.isCurrentUserAdmin()) {
            return SqlValidationResult.allowed("Administrator access");
        }

        List<UserPermission> userPermissions = permissionAuthService.getUserPermissions(userId, connectionId);

        // Extract tables and columns from SQL
        Set<String> referencedTables = extractTablesFromSql(sql);
        Set<String> referencedColumns = extractColumnsFromSql(sql);

        // Determine required permission type based on SQL operation
        PermissionType requiredPermission = determineRequiredPermission(sql);

        // Check table permissions
        for (String table : referencedTables) {
            String[] parts = table.split("\\.");
            String schemaName = parts.length > 1 ? parts[0] : null;
            String tableName = parts.length > 1 ? parts[1] : parts[0];

            boolean hasTablePermission = userPermissions.stream()
                    .anyMatch(perm -> matchesTablePermission(perm, requiredPermission, schemaName, tableName));

            if (!hasTablePermission) {
                return SqlValidationResult.denied("No " + requiredPermission + " permission for table: " + table);
            }
        }

        // For SELECT queries, also check column permissions if specific columns are referenced
        if (requiredPermission == PermissionType.READ && !referencedColumns.isEmpty()) {
            for (String column : referencedColumns) {
                boolean hasColumnPermission = userPermissions.stream()
                        .anyMatch(perm -> matchesColumnPermission(perm, requiredPermission, column));

                if (!hasColumnPermission) {
                    // Check if column access is granted via table-level permission
                    boolean hasTableLevelAccess = referencedTables.stream()
                            .anyMatch(table -> {
                                String[] parts = table.split("\\.");
                                String schemaName = parts.length > 1 ? parts[0] : null;
                                String tableName = parts.length > 1 ? parts[1] : parts[0];
                                return userPermissions.stream()
                                        .anyMatch(perm -> matchesTablePermission(perm, requiredPermission, schemaName, tableName));
                            });

                    if (!hasTableLevelAccess) {
                        return SqlValidationResult.denied("No " + requiredPermission + " permission for column: " + column);
                    }
                }
            }
        }

        return SqlValidationResult.allowed("All required permissions granted");
    }

    /**
     * Extract table references from SQL query
     */
    private Set<String> extractTablesFromSql(String sql) {
        Set<String> tables = Pattern.compile(TABLE_PATTERN.pattern(), Pattern.CASE_INSENSITIVE)
                .matcher(sql)
                .results()
                .map(result -> {
                    String schema = result.group(1);
                    String table = result.group(2);
                    return schema != null ? schema + "." + table : table;
                })
                .collect(Collectors.toSet());

        logger.debug("Extracted tables from SQL: {}", tables);
        return tables;
    }

    /**
     * Extract column references from SQL query (simplified approach)
     */
    private Set<String> extractColumnsFromSql(String sql) {
        Set<String> columns = Pattern.compile(COLUMN_PATTERN.pattern(), Pattern.CASE_INSENSITIVE)
                .matcher(sql)
                .results()
                .map(result -> result.group(1))
                .filter(col -> !isReservedKeyword(col))
                .collect(Collectors.toSet());

        logger.debug("Extracted columns from SQL: {}", columns);
        return columns;
    }

    /**
     * Determine required permission type based on SQL operation
     */
    private PermissionType determineRequiredPermission(String sql) {
        String upperSql = sql.toUpperCase().trim();

        if (upperSql.startsWith("SELECT")) {
            return PermissionType.READ;
        } else if (upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE")) {
            return PermissionType.WRITE;
        } else if (upperSql.startsWith("DELETE") || upperSql.startsWith("DROP") || upperSql.startsWith("TRUNCATE")) {
            return PermissionType.DELETE;
        } else if (upperSql.startsWith("CREATE") || upperSql.startsWith("ALTER")) {
            return PermissionType.ADMIN;
        }

        return PermissionType.READ; // Default to most restrictive
    }

    /**
     * Check if permission matches table requirement
     */
    private boolean matchesTablePermission(UserPermission permission, PermissionType requiredType,
                                           String schemaName, String tableName) {
        if (!permission.granted() || permission.permissionType() != requiredType) {
            return false;
        }

        // Check hierarchical permissions
        switch (permission.scope()) {
            case CONNECTION:
                return true; // Connection-level permission grants access to all tables
            case SCHEMA:
                return schemaName == null || schemaName.equals(permission.schemaName());
            case TABLE:
                return (schemaName == null || schemaName.equals(permission.schemaName())) &&
                        tableName.equals(permission.tableName());
            case COLUMN:
                return false; // Column permission doesn't grant table access
        }

        return false;
    }

    /**
     * Check if permission matches column requirement
     */
    private boolean matchesColumnPermission(UserPermission permission, PermissionType requiredType, String columnName) {
        if (!permission.granted() || permission.permissionType() != requiredType) {
            return false;
        }

        return permission.scope() == cherry.mastermeister.enums.PermissionScope.COLUMN &&
                columnName.equals(permission.columnName());
    }

    /**
     * Check if string is a SQL reserved keyword
     */
    private boolean isReservedKeyword(String word) {
        Set<String> keywords = Set.of(
                "SELECT", "FROM", "WHERE", "JOIN", "ON", "GROUP", "BY", "ORDER",
                "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT", "AS",
                "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL"
        );
        return keywords.contains(word.toUpperCase());
    }

    /**
     * Result of SQL validation
     */
    public static class SqlValidationResult {
        private final boolean allowed;
        private final String reason;

        private SqlValidationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static SqlValidationResult allowed(String reason) {
            return new SqlValidationResult(true, reason);
        }

        public static SqlValidationResult denied(String reason) {
            return new SqlValidationResult(false, reason);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }
}

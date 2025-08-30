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

import cherry.mastermeister.enums.DatabaseType;

/**
 * Utility class for SQL identifier escaping based on database type.
 * Different database systems use different quote characters for identifiers.
 */
public class SqlEscapeUtil {

    /**
     * Escape table name based on database type
     */
    public static String escapeTableName(String tableName, DatabaseType dbType) {
        return escapeIdentifier(tableName, dbType);
    }

    /**
     * Escape column name based on database type
     */
    public static String escapeColumnName(String columnName, DatabaseType dbType) {
        return escapeIdentifier(columnName, dbType);
    }

    /**
     * Escape schema name based on database type
     */
    public static String escapeSchemaName(String schemaName, DatabaseType dbType) {
        return escapeIdentifier(schemaName, dbType);
    }

    /**
     * Generic identifier escaping based on database type
     */
    private static String escapeIdentifier(String identifier, DatabaseType dbType) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }

        switch (dbType) {
            case MYSQL:
            case MARIADB:
                // MySQL and MariaDB use backticks
                return "`" + identifier.replace("`", "``") + "`";
            
            case POSTGRESQL:
            case H2:
            default:
                // PostgreSQL and H2 use double quotes (SQL standard)
                return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
    }

    /**
     * Get the quote character for the given database type
     */
    public static String getQuoteCharacter(DatabaseType dbType) {
        switch (dbType) {
            case MYSQL:
            case MARIADB:
                return "`";
            case POSTGRESQL:
            case H2:
            default:
                return "\"";
        }
    }

    /**
     * Check if identifier needs escaping (contains special characters)
     */
    public static boolean needsEscaping(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        
        // Check for spaces, special characters, or SQL keywords
        return identifier.contains(" ") 
            || identifier.contains("-") 
            || identifier.contains(".") 
            || !identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
            || isSqlKeyword(identifier.toLowerCase());
    }

    /**
     * Check if the identifier is a common SQL keyword
     */
    private static boolean isSqlKeyword(String identifier) {
        // Common SQL keywords that need escaping
        String[] keywords = {
            "select", "from", "where", "order", "group", "having", "insert", "update", "delete",
            "create", "drop", "alter", "table", "column", "index", "view", "database", "schema",
            "user", "role", "grant", "revoke", "commit", "rollback", "transaction", "begin", "end",
            "if", "else", "case", "when", "then", "union", "join", "left", "right", "inner", "outer",
            "exists", "in", "like", "between", "and", "or", "not", "null", "true", "false",
            "primary", "foreign", "key", "unique", "check", "default", "auto_increment", "serial"
        };
        
        for (String keyword : keywords) {
            if (keyword.equals(identifier)) {
                return true;
            }
        }
        return false;
    }
}
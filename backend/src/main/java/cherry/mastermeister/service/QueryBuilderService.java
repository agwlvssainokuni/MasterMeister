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
import cherry.mastermeister.model.RecordFilter;
import cherry.mastermeister.util.SqlEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QueryBuilderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Build SELECT query with filtering, sorting, and pagination
     */
    public QueryResult buildSelectQuery(String schemaName, String tableName,
                                        List<ColumnMetadata> columns, RecordFilter filter,
                                        int page, int pageSize, DatabaseType dbType) {

        StringBuilder query = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();

        // SELECT clause
        String columnList = columns.stream()
                .map(col -> SqlEscapeUtil.escapeColumnName(col.columnName(), dbType))
                .collect(Collectors.joining(", "));

        query.append("SELECT ").append(columnList).append(" FROM ")
                .append(buildTableName(schemaName, tableName, dbType));

        // WHERE clause
        if (filter.hasFilters()) {
            String whereClause = buildWhereClause(filter, parameters, dbType);
            if (!whereClause.isEmpty()) {
                query.append(" WHERE ").append(whereClause);
            }
        }

        // ORDER BY clause
        if (filter.hasSorting()) {
            String orderByClause = buildOrderByClause(filter, dbType);
            query.append(" ORDER BY ").append(orderByClause);
        }

        // LIMIT and OFFSET for pagination
        int offset = page * pageSize;
        query.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        logger.debug("Generated query: {}", query.toString());
        logger.debug("Parameters: {}", parameters);

        return new QueryResult(query.toString(), parameters);
    }

    /**
     * Build COUNT query for total records with same filters
     */
    public QueryResult buildCountQuery(String schemaName, String tableName, RecordFilter filter, DatabaseType dbType) {
        StringBuilder query = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();

        query.append("SELECT COUNT(*) FROM ").append(buildTableName(schemaName, tableName, dbType));

        // WHERE clause (same as SELECT query)
        if (filter.hasFilters()) {
            String whereClause = buildWhereClause(filter, parameters, dbType);
            if (!whereClause.isEmpty()) {
                query.append(" WHERE ").append(whereClause);
            }
        }

        return new QueryResult(query.toString(), parameters);
    }

    /**
     * Build WHERE clause from filter conditions
     */
    private String buildWhereClause(RecordFilter filter, Map<String, Object> parameters, DatabaseType dbType) {
        List<String> conditions = new ArrayList<>();

        // Column-specific filters
        if (filter.columnFilters() != null) {
            for (int i = 0; i < filter.columnFilters().size(); i++) {
                RecordFilter.ColumnFilter columnFilter = filter.columnFilters().get(i);
                String condition = buildColumnCondition(columnFilter, parameters, i, dbType);
                if (condition != null && !condition.isEmpty()) {
                    conditions.add(condition);
                }
            }
        }

        // Custom WHERE clause
        if (filter.customWhere() != null && !filter.customWhere().trim().isEmpty()) {
            conditions.add("(" + filter.customWhere().trim() + ")");
        }

        return String.join(" AND ", conditions);
    }

    /**
     * Build condition for single column filter
     */
    private String buildColumnCondition(RecordFilter.ColumnFilter columnFilter,
                                        Map<String, Object> parameters, int index, DatabaseType dbType) {
        String columnName = SqlEscapeUtil.escapeColumnName(columnFilter.columnName(), dbType);
        String paramName = "param" + index;

        switch (columnFilter.operator()) {
            case EQUALS:
                parameters.put(paramName, columnFilter.value());
                return columnName + " = :" + paramName;

            case NOT_EQUALS:
                parameters.put(paramName, columnFilter.value());
                return columnName + " != :" + paramName;

            case GREATER_THAN:
                parameters.put(paramName, columnFilter.value());
                return columnName + " > :" + paramName;

            case GREATER_EQUAL:
                parameters.put(paramName, columnFilter.value());
                return columnName + " >= :" + paramName;

            case LESS_THAN:
                parameters.put(paramName, columnFilter.value());
                return columnName + " < :" + paramName;

            case LESS_EQUAL:
                parameters.put(paramName, columnFilter.value());
                return columnName + " <= :" + paramName;

            case LIKE:
                parameters.put(paramName, "%" + columnFilter.value() + "%");
                return columnName + " LIKE :" + paramName;

            case NOT_LIKE:
                parameters.put(paramName, "%" + columnFilter.value() + "%");
                return columnName + " NOT LIKE :" + paramName;

            case BETWEEN:
                parameters.put(paramName + "_1", columnFilter.value());
                parameters.put(paramName + "_2", columnFilter.value2());
                return columnName + " BETWEEN :" + paramName + "_1 AND :" + paramName + "_2";

            case IS_NULL:
                return columnName + " IS NULL";

            case IS_NOT_NULL:
                return columnName + " IS NOT NULL";

            case IN:
                if (columnFilter.value() instanceof List<?> list && !list.isEmpty()) {
                    parameters.put(paramName, list);
                    return columnName + " IN (:" + paramName + ")";
                }
                return null;

            case NOT_IN:
                if (columnFilter.value() instanceof List<?> list && !list.isEmpty()) {
                    parameters.put(paramName, list);
                    return columnName + " NOT IN (:" + paramName + ")";
                }
                return null;

            default:
                logger.warn("Unsupported filter operator: {}", columnFilter.operator());
                return null;
        }
    }

    /**
     * Build ORDER BY clause
     */
    private String buildOrderByClause(RecordFilter filter, DatabaseType dbType) {
        return filter.sortOrders().stream()
                .map(order -> SqlEscapeUtil.escapeColumnName(order.columnName(), dbType) + " " + order.direction().name())
                .collect(Collectors.joining(", "));
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
     * Result containing generated query and parameters
     */
    public record QueryResult(
            String query,
            Map<String, Object> parameters
    ) {
    }
}

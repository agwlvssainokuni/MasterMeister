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

package cherry.mastermeister.model;

import java.util.List;

/**
 * Model representing filtering conditions for record queries
 */
public record RecordFilter(
        List<ColumnFilter> columnFilters,
        String customWhere,
        List<SortOrder> sortOrders
) {
    public static RecordFilter empty() {
        return new RecordFilter(List.of(), null, List.of());
    }

    public boolean hasFilters() {
        return (columnFilters != null && !columnFilters.isEmpty()) ||
                (customWhere != null && !customWhere.trim().isEmpty());
    }

    public boolean hasSorting() {
        return sortOrders != null && !sortOrders.isEmpty();
    }

    /**
     * Column-specific filter condition
     */
    public record ColumnFilter(
            String columnName,
            FilterOperator operator,
            Object value,
            Object value2  // For BETWEEN operator
    ) {
    }

    /**
     * Sort order specification
     */
    public record SortOrder(
            String columnName,
            SortDirection direction
    ) {
    }

    /**
     * Filter operators for different data types
     */
    public enum FilterOperator {
        EQUALS,         // =
        NOT_EQUALS,     // !=
        GREATER_THAN,   // >
        GREATER_EQUALS, // >=
        LESS_THAN,      // <
        LESS_EQUALS,    // <=
        LIKE,           // LIKE (for strings)
        NOT_LIKE,       // NOT LIKE
        IN,             // IN (list)
        NOT_IN,         // NOT IN (list)
        BETWEEN,        // BETWEEN (range)
        IS_NULL,        // IS NULL
        IS_NOT_NULL     // IS NOT NULL
    }

    /**
     * Sort directions
     */
    public enum SortDirection {
        ASC,
        DESC
    }
}

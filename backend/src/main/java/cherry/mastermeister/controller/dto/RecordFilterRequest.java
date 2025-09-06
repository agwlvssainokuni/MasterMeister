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

package cherry.mastermeister.controller.dto;

import java.util.List;

/**
 * DTO for record filtering requests from frontend
 */
public record RecordFilterRequest(
        List<ColumnFilterRequest> columnFilters,
        String customWhere,
        List<SortOrderRequest> sortOrders
) {
    public static RecordFilterRequest empty() {
        return new RecordFilterRequest(List.of(), null, List.of());
    }

    /**
     * Column filter request
     */
    public record ColumnFilterRequest(
            String columnName,
            String operator,  // String representation of FilterOperator
            Object value,
            Object value2     // For BETWEEN operator
    ) {
    }

    /**
     * Sort order request
     */
    public record SortOrderRequest(
            String columnName,
            String direction  // "ASC" or "DESC"
    ) {
    }
}

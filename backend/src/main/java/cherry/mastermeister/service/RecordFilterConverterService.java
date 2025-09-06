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

import cherry.mastermeister.controller.dto.RecordFilterRequest;
import cherry.mastermeister.model.RecordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordFilterConverterService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Convert RecordFilterRequest to RecordFilter
     */
    public RecordFilter convertFromRequest(RecordFilterRequest request) {
        if (request == null) {
            return RecordFilter.empty();
        }

        List<RecordFilter.ColumnFilter> columnFilters = convertColumnFilters(request.columnFilters());
        List<RecordFilter.SortOrder> sortOrders = convertSortOrders(request.sortOrders());

        return new RecordFilter(columnFilters, request.customWhere(), sortOrders);
    }

    /**
     * Convert column filter requests to model objects
     */
    private List<RecordFilter.ColumnFilter> convertColumnFilters(List<RecordFilterRequest.ColumnFilterRequest> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
                .map(this::convertColumnFilter)
                .filter(filter -> filter != null)
                .collect(Collectors.toList());
    }

    /**
     * Convert single column filter request
     */
    private RecordFilter.ColumnFilter convertColumnFilter(RecordFilterRequest.ColumnFilterRequest request) {
        try {
            RecordFilter.FilterOperator operator = RecordFilter.FilterOperator.valueOf(
                    request.operator().toUpperCase());

            return new RecordFilter.ColumnFilter(
                    request.columnName(),
                    operator,
                    request.value(),
                    request.value2()
            );
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid filter operator: {}", request.operator());
            return null;
        }
    }

    /**
     * Convert sort order requests to model objects
     */
    private List<RecordFilter.SortOrder> convertSortOrders(List<RecordFilterRequest.SortOrderRequest> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
                .map(this::convertSortOrder)
                .filter(order -> order != null)
                .collect(Collectors.toList());
    }

    /**
     * Convert single sort order request
     */
    private RecordFilter.SortOrder convertSortOrder(RecordFilterRequest.SortOrderRequest request) {
        try {
            RecordFilter.SortDirection direction = RecordFilter.SortDirection.valueOf(
                    request.direction().toUpperCase());

            return new RecordFilter.SortOrder(request.columnName(), direction);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid sort direction: {}", request.direction());
            return null;
        }
    }
}

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
 * Model representing the result of a record query with metadata
 */
public record RecordQueryResult(
        List<TableRecord> records,
        List<AccessibleColumn> accessibleColumns,
        long totalRecords,
        int currentPage,
        int pageSize,
        long executionTimeMs,
        String query
) {
    /**
     * Check if result has more pages
     */
    public boolean hasNextPage() {
        return (long) currentPage * pageSize < totalRecords;
    }

    /**
     * Check if result has previous pages
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Get total number of pages
     */
    public long getTotalPages() {
        return (totalRecords + pageSize - 1) / pageSize;
    }

    /**
     * Check if this is a large dataset (for audit logging)
     */
    public boolean isLargeDataset(int threshold) {
        return records.size() >= threshold;
    }
}

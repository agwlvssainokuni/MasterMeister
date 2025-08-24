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

import java.util.Map;

/**
 * Model representing a single record from a database table
 */
public record TableRecord(
        Map<String, Object> data,
        Map<String, String> columnTypes,
        Map<String, Boolean> columnPermissions
) {
    /**
     * Get value for specific column
     */
    public Object getValue(String columnName) {
        return data.get(columnName);
    }

    /**
     * Check if user can read specific column
     */
    public boolean canReadColumn(String columnName) {
        return columnPermissions.getOrDefault(columnName, false);
    }

    /**
     * Get filtered data containing only readable columns
     */
    public Map<String, Object> getReadableData() {
        return data.entrySet().stream()
                .filter(entry -> canReadColumn(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}

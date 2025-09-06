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

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO for record creation requests from frontend
 */
public record RecordCreateRequest(
        @NotNull(message = "Record data is required")
        Map<String, Object> data
) {

    /**
     * Get column value, handling null cases
     */
    public Object getColumnValue(String columnName) {
        return data != null ? data.get(columnName) : null;
    }

    /**
     * Check if column is provided in request
     */
    public boolean hasColumn(String columnName) {
        return data != null && data.containsKey(columnName);
    }
}

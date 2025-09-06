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
 * DTO for record deletion requests from frontend
 */
public record RecordDeleteSpec(
        @NotNull(message = "WHERE conditions are required")
        Map<String, Object> whereConditions,

        boolean skipReferentialIntegrityCheck
) {

    /**
     * Get WHERE condition value
     */
    public Object getWhereValue(String columnName) {
        return whereConditions != null ? whereConditions.get(columnName) : null;
    }

    /**
     * Check if column is in WHERE condition
     */
    public boolean hasWhereColumn(String columnName) {
        return whereConditions != null && whereConditions.containsKey(columnName);
    }

    /**
     * Create request with referential integrity check enabled (default)
     */
    public static RecordDeleteSpec withIntegrityCheck(Map<String, Object> whereConditions) {
        return new RecordDeleteSpec(whereConditions, false);
    }

    /**
     * Create request with referential integrity check disabled (use with caution)
     */
    public static RecordDeleteSpec skipIntegrityCheck(Map<String, Object> whereConditions) {
        return new RecordDeleteSpec(whereConditions, true);
    }
}

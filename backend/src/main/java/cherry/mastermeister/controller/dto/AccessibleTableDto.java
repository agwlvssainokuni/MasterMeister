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

import java.util.Set;

/**
 * DTO for accessible table information with permission details
 */
public record AccessibleTableDto(
        Long connectionId,
        String schemaName,
        String tableName,
        String fullTableName,
        String tableType,
        String comment,
        Set<String> permissions,
        boolean canRead,
        boolean canWrite,
        boolean canDelete,
        boolean canAdmin,
        boolean canModifyData,
        boolean canPerformCrud
) {
}

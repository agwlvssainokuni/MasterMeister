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

import cherry.mastermeister.enums.BulkPermissionScope;
import cherry.mastermeister.enums.PermissionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for bulk permission operations
 */
public record BulkPermissionRequest(
        @NotNull
        BulkPermissionScope scope,
        
        @NotNull
        @Size(min = 1)
        List<PermissionType> permissionTypes,
        
        List<String> userEmails,
        
        List<String> schemaNames,
        
        List<String> tableNames,
        
        boolean includeSystemTables,
        
        @Size(max = 500)
        String description
) {
}
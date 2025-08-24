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

import cherry.mastermeister.enums.PermissionType;

import java.util.Set;

/**
 * Model representing a table accessible to the current user with permission information
 */
public record AccessibleTable(
        Long connectionId,
        String schemaName,
        String tableName,
        String tableType,
        String comment,
        Set<PermissionType> permissions,
        boolean hasReadPermission,
        boolean hasWritePermission,
        boolean hasDeletePermission,
        boolean hasAdminPermission
) {
    /**
     * Create AccessibleTable with calculated permission flags
     */
    public static AccessibleTable of(TableMetadata tableInfo, Long connectionId, Set<PermissionType> permissions) {
        return new AccessibleTable(
                connectionId,
                tableInfo.schema(),
                tableInfo.tableName(),
                tableInfo.tableType(),
                tableInfo.comment(),
                permissions,
                permissions.contains(PermissionType.READ),
                permissions.contains(PermissionType.WRITE),
                permissions.contains(PermissionType.DELETE),
                permissions.contains(PermissionType.ADMIN)
        );
    }

    /**
     * Get full table identifier
     */
    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Check if user can perform CRUD operations
     */
    public boolean canPerformCrud() {
        return hasReadPermission && hasWritePermission && hasDeletePermission;
    }

    /**
     * Check if user can modify data (read and write)
     */
    public boolean canModifyData() {
        return hasReadPermission && hasWritePermission;
    }
}

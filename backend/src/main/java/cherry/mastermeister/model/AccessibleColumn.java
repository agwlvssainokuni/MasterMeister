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
 * Model representing a column accessible to the current user with permission information
 */
public record AccessibleColumn(
        String columnName,
        String dataType,
        Integer columnSize,
        Integer decimalDigits,
        Boolean nullable,
        String defaultValue,
        String comment,
        Boolean primaryKey,
        Boolean autoIncrement,
        Integer ordinalPosition,
        Set<PermissionType> permissions,
        boolean hasReadPermission,
        boolean hasWritePermission,
        boolean hasDeletePermission,
        boolean hasAdminPermission
) {
    /**
     * Create AccessibleColumn with calculated permission flags
     */
    public static AccessibleColumn of(ColumnMetadata columnInfo, Set<PermissionType> permissions) {
        return new AccessibleColumn(
                columnInfo.columnName(),
                columnInfo.dataType(),
                columnInfo.columnSize(),
                columnInfo.decimalDigits(),
                columnInfo.nullable(),
                columnInfo.defaultValue(),
                columnInfo.comment(),
                columnInfo.primaryKey(),
                columnInfo.autoIncrement(),
                columnInfo.ordinalPosition(),
                permissions,
                permissions.contains(PermissionType.READ),
                permissions.contains(PermissionType.WRITE),
                permissions.contains(PermissionType.DELETE),
                permissions.contains(PermissionType.ADMIN)
        );
    }
}
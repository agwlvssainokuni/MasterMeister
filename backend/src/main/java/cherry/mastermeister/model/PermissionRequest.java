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

public record PermissionRequest(
        Long userId,
        String userEmail,
        Long connectionId,
        PermissionType permissionType,
        String schemaName,
        String tableName,
        String columnName
) {

    public static PermissionRequest connection(
            Long userId, String userEmail, Long connectionId,
            PermissionType permissionType
    ) {
        return new PermissionRequest(userId, userEmail, connectionId, permissionType, null, null, null);
    }

    public static PermissionRequest schema(
            Long userId, String userEmail, Long connectionId,
            PermissionType permissionType,
            String schemaName
    ) {
        return new PermissionRequest(userId, userEmail, connectionId, permissionType, schemaName, null, null);
    }

    public static PermissionRequest table(
            Long userId, String userEmail, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName
    ) {
        return new PermissionRequest(userId, userEmail, connectionId, permissionType, schemaName, tableName, null);
    }

    public static PermissionRequest column(
            Long userId, String userEmail, Long connectionId,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName
    ) {
        return new PermissionRequest(userId, userEmail, connectionId, permissionType, schemaName, tableName, columnName);
    }
}

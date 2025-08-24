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

package cherry.mastermeister.exception;

import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.model.PermissionCheckResult;

public class PermissionDeniedException extends RuntimeException {

    private final PermissionType requiredPermission;
    private final Long connectionId;
    private final String schemaName;
    private final String tableName;
    private final String columnName;
    private final PermissionCheckResult checkResult;

    public PermissionDeniedException(
            String message,
            PermissionType requiredPermission,
            Long connectionId, String schemaName, String tableName,
            String columnName, PermissionCheckResult checkResult
    ) {
        super(message);
        this.requiredPermission = requiredPermission;
        this.connectionId = connectionId;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.checkResult = checkResult;
    }

    public PermissionDeniedException(
            String message,
            PermissionCheckResult checkResult
    ) {
        super(message + (checkResult != null ? ": " + checkResult.reason() : ""));
        this.requiredPermission = null;
        this.connectionId = null;
        this.schemaName = null;
        this.tableName = null;
        this.columnName = null;
        this.checkResult = checkResult;
    }

    public PermissionType getRequiredPermission() {
        return requiredPermission;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public PermissionCheckResult getCheckResult() {
        return checkResult;
    }
}

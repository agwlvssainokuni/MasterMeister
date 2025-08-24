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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record PermissionExportData(
        @JsonProperty("export_info") ExportInfo exportInfo,
        @JsonProperty("connection_info") ConnectionInfo connectionInfo,
        @JsonProperty("users") List<UserPermissionData> users,
        @JsonProperty("templates") List<TemplateData> templates
) {

    public record ExportInfo(
            @JsonProperty("version") String version,
            @JsonProperty("exported_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime exportedAt,
            @JsonProperty("exported_by") String exportedBy,
            @JsonProperty("description") String description
    ) {
        public ExportInfo(LocalDateTime exportedAt, String exportedBy, String description) {
            this("1.0", exportedAt, exportedBy, description);
        }
    }

    public record ConnectionInfo(
            @JsonProperty("connection_id") Long connectionId,
            @JsonProperty("connection_name") String connectionName,
            @JsonProperty("database_type") String databaseType,
            @JsonProperty("database_name") String databaseName
    ) {
    }

    public record UserPermissionData(
            @JsonProperty("user_email") String userEmail,
            @JsonProperty("permissions") List<PermissionData> permissions
    ) {
    }

    public record TemplateData(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("is_active") Boolean isActive,
            @JsonProperty("items") List<PermissionData> items
    ) {
    }

    public record PermissionData(
            @JsonProperty("scope") String scope,
            @JsonProperty("permission_type") String permissionType,
            @JsonProperty("schema_name") String schemaName,
            @JsonProperty("table_name") String tableName,
            @JsonProperty("column_name") String columnName,
            @JsonProperty("granted") Boolean granted,
            @JsonProperty("expires_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime expiresAt,
            @JsonProperty("comment") String comment
    ) {
    }
}

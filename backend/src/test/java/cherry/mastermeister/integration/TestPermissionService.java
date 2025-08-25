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

package cherry.mastermeister.integration;

import cherry.mastermeister.annotation.RequirePermission;
import cherry.mastermeister.enums.PermissionType;
import org.springframework.stereotype.Service;

@Service
public class TestPermissionService {

    @RequirePermission(value = PermissionType.READ, schemaNameParam = "schemaName", tableNameParam = "tableName")
    public void readTable(Long connectionId, String schemaName, String tableName) {
        // Test method for permission validation
    }

    @RequirePermission(value = PermissionType.WRITE, schemaNameParam = "schemaName", tableNameParam = "tableName", columnNameParam = "columnName")
    public void writeColumn(Long connectionId, String schemaName, String tableName, String columnName) {
        // Test method for column-level permission validation
    }

    @RequirePermission(value = PermissionType.DELETE, schemaNameParam = "schemaName", tableNameParam = "tableName")
    public void deleteFromTable(Long connectionId, String schemaName, String tableName) {
        // Test method for delete permission validation
    }

    @RequirePermission(PermissionType.ADMIN)
    public void adminOperation(Long connectionId) {
        // Test method for admin permission validation
    }
}

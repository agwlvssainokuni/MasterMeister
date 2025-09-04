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

import cherry.mastermeister.entity.DatabaseConnectionEntity;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.entity.UserPermissionEntity;
import cherry.mastermeister.enums.*;
import cherry.mastermeister.repository.DatabaseConnectionRepository;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import cherry.mastermeister.service.PermissionService;
import cherry.mastermeister.util.SqlPermissionFilter;
import cherry.mastermeister.util.SqlPermissionFilter.SqlValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PermissionValidationIntegrationTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SqlPermissionFilter sqlPermissionFilter;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseConnectionRepository databaseConnectionRepository;

    private UserEntity normalUser;
    private UserEntity adminUser;
    private DatabaseConnectionEntity testConnection;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        userPermissionRepository.deleteAll();
        userRepository.deleteAll();
        databaseConnectionRepository.deleteAll();

        // Create normal user
        normalUser = new UserEntity();
        normalUser.setEmail("normal-" + System.nanoTime() + "@example.com");
        normalUser.setPassword("password123"); // Required field
        normalUser.setStatus(UserStatus.APPROVED);
        normalUser.setRole(UserRole.USER);
        normalUser.setCreatedAt(LocalDateTime.now());
        normalUser = userRepository.save(normalUser);

        // Create admin user
        adminUser = new UserEntity();
        adminUser.setEmail("admin-" + System.nanoTime() + "@example.com");
        adminUser.setPassword("admin123"); // Required field
        adminUser.setStatus(UserStatus.APPROVED);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        // Create test database connection
        testConnection = new DatabaseConnectionEntity();
        testConnection.setName("Test Database");
        testConnection.setDbType(DatabaseType.H2);
        testConnection.setHost("mem");
        testConnection.setPort(9092); // Required field
        testConnection.setDatabaseName("testdb");
        testConnection.setUsername("sa");
        testConnection.setPassword("");
        testConnection.setActive(true);
        testConnection.setCreatedAt(LocalDateTime.now());
        testConnection.setUpdatedAt(LocalDateTime.now());
        testConnection = databaseConnectionRepository.save(testConnection);

        // Clear security context
        SecurityContextHolder.clearContext();
    }



    @Test
    @Disabled("SqlPermissionFilter tests excluded")
    void testSqlPermissionFiltering() {
        // Setup permissions for SQL filtering test
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.READ, "PUBLIC", "users", null, true);
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.READ, "PUBLIC", "products", null, false);
        createPermission(normalUser, PermissionScope.COLUMN, PermissionType.READ, "PUBLIC", "users", "email", false);

        // Test simple SELECT (allowed)
        String sql1 = "SELECT id, name FROM users";
        SqlValidationResult result1 = sqlPermissionFilter.validateSqlQuery(sql1, normalUser.getId(), testConnection.getId());
        assertTrue(result1.isAllowed());

        // Test SELECT with denied column (should fail)
        String sql2 = "SELECT id, name, email FROM users";
        SqlValidationResult result2 = sqlPermissionFilter.validateSqlQuery(sql2, normalUser.getId(), testConnection.getId());
        assertFalse(result2.isAllowed());

        // Test SELECT from denied table (should fail)
        String sql3 = "SELECT * FROM products";
        SqlValidationResult result3 = sqlPermissionFilter.validateSqlQuery(sql3, normalUser.getId(), testConnection.getId());
        assertFalse(result3.isAllowed());

        // Test JOIN with mixed permissions (should fail due to products table)
        String sql4 = "SELECT u.name, p.price FROM users u JOIN products p ON u.id = p.user_id";
        SqlValidationResult result4 = sqlPermissionFilter.validateSqlQuery(sql4, normalUser.getId(), testConnection.getId());
        assertFalse(result4.isAllowed());
    }

    @Test
    @Disabled("SqlPermissionFilter tests excluded")
    void testSqlWritePermissions() {
        // Setup write permissions
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.WRITE, "PUBLIC", "users", null, true);
        createPermission(normalUser, PermissionScope.COLUMN, PermissionType.WRITE, "PUBLIC", "users", "email", false);

        // Test INSERT (should work)
        String sql1 = "INSERT INTO users (name, age) VALUES ('John', 25)";
        SqlValidationResult insertResult1 = sqlPermissionFilter.validateSqlQuery(sql1, normalUser.getId(), testConnection.getId());
        assertTrue(insertResult1.isAllowed());

        // Test INSERT with denied column (should fail)
        String sql2 = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        SqlValidationResult insertResult2 = sqlPermissionFilter.validateSqlQuery(sql2, normalUser.getId(), testConnection.getId());
        assertFalse(insertResult2.isAllowed());

        // Test UPDATE (should work for allowed columns)
        String sql3 = "UPDATE users SET name = 'Jane' WHERE id = 1";
        SqlValidationResult updateResult1 = sqlPermissionFilter.validateSqlQuery(sql3, normalUser.getId(), testConnection.getId());
        assertTrue(updateResult1.isAllowed());

        // Test UPDATE with denied column (should fail)
        String sql4 = "UPDATE users SET email = 'jane@example.com' WHERE id = 1";
        SqlValidationResult updateResult2 = sqlPermissionFilter.validateSqlQuery(sql4, normalUser.getId(), testConnection.getId());
        assertFalse(updateResult2.isAllowed());
    }

    @Test
    @Disabled("SqlPermissionFilter tests excluded")
    void testSqlDeletePermissions() {
        // Setup delete permission on one table but not another
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.DELETE, "PUBLIC", "temp_data", null, true);

        // Test DELETE on allowed table
        String sql1 = "DELETE FROM temp_data WHERE created_at < '2025-01-01'";
        SqlValidationResult deleteResult1 = sqlPermissionFilter.validateSqlQuery(sql1, normalUser.getId(), testConnection.getId());
        assertTrue(deleteResult1.isAllowed());

        // Test DELETE on table without permission (should fail)
        String sql2 = "DELETE FROM users WHERE active = false";
        SqlValidationResult deleteResult2 = sqlPermissionFilter.validateSqlQuery(sql2, normalUser.getId(), testConnection.getId());
        assertFalse(deleteResult2.isAllowed());
    }


    private void createPermission(
            UserEntity user, PermissionScope scope, PermissionType permissionType,
            String schemaName, String tableName, String columnName, boolean granted
    ) {
        createPermissionWithExpiry(
                user, scope, permissionType,
                schemaName, tableName, columnName,
                granted, null
        );
    }

    private void createPermissionWithExpiry(
            UserEntity user, PermissionScope scope, PermissionType permissionType,
            String schemaName, String tableName, String columnName,
            boolean granted, LocalDateTime expiresAt
    ) {
        UserPermissionEntity permission = new UserPermissionEntity();
        permission.setUser(user);
        permission.setConnectionId(testConnection.getId());
        permission.setScope(scope);
        permission.setPermissionType(permissionType);
        permission.setSchemaName(schemaName);
        permission.setTableName(tableName);
        permission.setColumnName(columnName);
        permission.setGranted(granted);
        permission.setGrantedBy("admin@example.com");
        permission.setGrantedAt(LocalDateTime.now());
        permission.setExpiresAt(expiresAt);
        userPermissionRepository.save(permission);
    }
}

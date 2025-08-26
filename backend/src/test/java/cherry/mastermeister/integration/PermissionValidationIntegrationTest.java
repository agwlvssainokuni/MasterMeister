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
import cherry.mastermeister.exception.PermissionDeniedException;
import cherry.mastermeister.model.PermissionCheckResult;
import cherry.mastermeister.model.PermissionRequest;
import cherry.mastermeister.repository.DatabaseConnectionRepository;
import cherry.mastermeister.repository.UserPermissionRepository;
import cherry.mastermeister.repository.UserRepository;
import cherry.mastermeister.service.PermissionAuthService;
import cherry.mastermeister.util.SqlPermissionFilter;
import cherry.mastermeister.util.SqlPermissionFilter.SqlValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PermissionValidationIntegrationTest {

    @Autowired
    private PermissionAuthService permissionAuthService;

    @Autowired
    private SqlPermissionFilter sqlPermissionFilter;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseConnectionRepository databaseConnectionRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private UserEntity normalUser;
    private UserEntity adminUser;
    private DatabaseConnectionEntity testConnection;

    @Autowired
    private TestPermissionService testPermissionService;

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
    void testHierarchicalPermissionResolution() {
        // Setup hierarchical permissions
        createPermission(normalUser, PermissionScope.CONNECTION, PermissionType.READ, null, null, null, true);
        createPermission(normalUser, PermissionScope.SCHEMA, PermissionType.WRITE, "PUBLIC", null, null, false); // Denied
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.WRITE, "PUBLIC", "users", null, true); // Override

        // Test CONNECTION level permission (should work)
        PermissionCheckResult connectionResult = permissionAuthService.checkPermission(
                PermissionRequest.connection(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.READ
                ));
        assertTrue(connectionResult.granted());

        // Test SCHEMA level WRITE denied
        PermissionCheckResult schemaDenied = permissionAuthService.checkPermission(
                PermissionRequest.schema(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC"
                )
        );
        assertFalse(schemaDenied.granted());

        // Test TABLE level WRITE override (should work despite schema denial)
        PermissionCheckResult tableAllowed = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users"
                )
        );
        assertTrue(tableAllowed.granted());

        // Test TABLE level WRITE for different table (should be denied by schema rule)
        PermissionCheckResult tableDenied = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "products"
                )
        );
        assertFalse(tableDenied.granted());
    }

    @Test
    void testColumnLevelPermissionSpecificity() {
        // Setup column-specific permissions
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.READ, "PUBLIC", "users", null, true);
        createPermission(normalUser, PermissionScope.COLUMN, PermissionType.WRITE, "PUBLIC", "users", "email", false); // Deny email write
        createPermission(normalUser, PermissionScope.COLUMN, PermissionType.WRITE, "PUBLIC", "users", "name", true); // Allow name write

        // Table level READ should work
        PermissionCheckResult tableReadResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.READ,
                        "PUBLIC", "users"
                )
        );
        assertTrue(tableReadResult.granted());

        // Column level email WRITE should be denied
        PermissionCheckResult emailWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.column(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users", "email"
                )
        );
        assertFalse(emailWriteResult.granted());

        // Column level name WRITE should be allowed
        PermissionCheckResult nameWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.column(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users", "name"
                )
        );
        assertTrue(nameWriteResult.granted());

        // Column without specific permission should inherit table permission (none for WRITE)
        PermissionCheckResult idWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.column(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users", "id"
                )
        );
        assertFalse(idWriteResult.granted());
    }

    @Test
    void testAdminUserFullAccess() {
        // Admin user should have full access without explicit permissions
        PermissionCheckResult adminReadResult = permissionAuthService.checkPermission(
                PermissionRequest.connection(
                        adminUser.getId(), adminUser.getEmail(), testConnection.getId(),
                        PermissionType.READ
                )
        );
        assertTrue(adminReadResult.granted());

        PermissionCheckResult adminWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        adminUser.getId(), adminUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users"
                )
        );
        assertTrue(adminWriteResult.granted());

        PermissionCheckResult adminDeleteResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        adminUser.getId(), adminUser.getEmail(), testConnection.getId(),
                        PermissionType.DELETE,
                        "PUBLIC", "logs"
                )
        );
        assertTrue(adminDeleteResult.granted());

        PermissionCheckResult adminResult = permissionAuthService.checkPermission(
                PermissionRequest.connection(
                        adminUser.getId(), adminUser.getEmail(), testConnection.getId(),
                        PermissionType.ADMIN
                )
        );
        assertTrue(adminResult.granted());
    }

    @Test
    void testExpiredPermissions() {
        // Create expired permission
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        createPermissionWithExpiry(normalUser, PermissionScope.TABLE, PermissionType.READ,
                "PUBLIC", "users", null, true, pastDate);

        // Expired permission should not grant access
        PermissionCheckResult expiredResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.READ,
                        "PUBLIC", "users"
                )
        );
        assertFalse(expiredResult.granted());
    }

    @Test
    void testFuturePermissions() {
        // Create future permission (not yet active)
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        UserPermissionEntity permission = new UserPermissionEntity();
        permission.setUser(normalUser);
        permission.setConnectionId(testConnection.getId());
        permission.setScope(PermissionScope.TABLE);
        permission.setPermissionType(PermissionType.READ);
        permission.setSchemaName("PUBLIC");
        permission.setTableName("users");
        permission.setGranted(true);
        permission.setGrantedBy("admin@example.com");
        permission.setGrantedAt(futureDate); // Future granted date
        permission.setExpiresAt(null);
        userPermissionRepository.save(permission);

        // Future permission should not grant access yet
        PermissionCheckResult futureResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.READ,
                        "PUBLIC", "users"
                )
        );
        assertFalse(futureResult.granted());
    }

    @Test
    void testAnnotationBasedPermissionValidation() {
        // Setup permissions for annotation tests
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.READ, "PUBLIC", "users", null, true);
        createPermission(normalUser, PermissionScope.COLUMN, PermissionType.WRITE, "PUBLIC", "users", "name", true);

        // Set security context for normal user
        setSecurityContext(normalUser);

        // Test successful READ operation
        assertDoesNotThrow(() -> {
            testPermissionService.readTable(testConnection.getId(), "PUBLIC", "users");
        });

        // Test successful WRITE operation
        assertDoesNotThrow(() -> {
            testPermissionService.writeColumn(testConnection.getId(), "PUBLIC", "users", "name");
        });

        // Test failed DELETE operation (no permission)
        assertThrows(PermissionDeniedException.class, () -> {
            testPermissionService.deleteFromTable(testConnection.getId(), "PUBLIC", "users");
        });

        // Test failed ADMIN operation (no admin permission)
        assertThrows(PermissionDeniedException.class, () -> {
            testPermissionService.adminOperation(testConnection.getId());
        });
    }

    @Test
    void testAnnotationBasedAdminAccess() {
        // Set security context for admin user
        setSecurityContext(adminUser);

        // Admin should pass all permission checks
        assertDoesNotThrow(() -> {
            testPermissionService.readTable(testConnection.getId(), "PUBLIC", "users");
        });

        assertDoesNotThrow(() -> {
            testPermissionService.writeColumn(testConnection.getId(), "PUBLIC", "users", "email");
        });

        assertDoesNotThrow(() -> {
            testPermissionService.deleteFromTable(testConnection.getId(), "PUBLIC", "logs");
        });

        assertDoesNotThrow(() -> {
            testPermissionService.adminOperation(testConnection.getId());
        });
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

    @Test
    void testComplexPermissionScenario() {
        // Setup complex permission hierarchy
        createPermission(normalUser, PermissionScope.CONNECTION, PermissionType.READ, null, null, null, true); // Global read
        createPermission(normalUser, PermissionScope.SCHEMA, PermissionType.WRITE, "PRIVATE", null, null, false); // Deny private schema write
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.WRITE, "PRIVATE", "user_settings", null, true); // Exception for user_settings
        createPermission(normalUser, PermissionScope.TABLE, PermissionType.DELETE, "PUBLIC", "audit_log", null, false); // Deny audit log delete
        createPermission(normalUser, PermissionScope.COLUMN, PermissionType.WRITE, "PUBLIC", "users", "password", false); // Deny password write

        // Test global read (should work everywhere)
        PermissionCheckResult tableReadResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.READ,
                        "PUBLIC", "users"
                )
        );
        assertTrue(tableReadResult.granted());
        PermissionCheckResult privateSettingsReadResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.READ,
                        "PRIVATE", "settings"
                )
        );
        assertTrue(privateSettingsReadResult.granted());

        // Test private schema write denial with exception
        PermissionCheckResult privateSecretsWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PRIVATE", "secrets"
                )
        );
        assertFalse(privateSecretsWriteResult.granted());
        PermissionCheckResult userSettingsWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PRIVATE", "user_settings"
                )
        );
        assertTrue(userSettingsWriteResult.granted());

        // Test specific table delete denial
        PermissionCheckResult auditLogDeleteResult = permissionAuthService.checkPermission(
                PermissionRequest.table(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.DELETE,
                        "PUBLIC", "audit_log"
                )
        );
        assertFalse(auditLogDeleteResult.granted());

        // Test column-specific write denial
        PermissionCheckResult passwordWriteResult = permissionAuthService.checkPermission(
                PermissionRequest.column(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users", "password"
                )
        );
        assertFalse(passwordWriteResult.granted());

        // But other columns should inherit table permissions (which inherit from connection read, but no connection write)
        PermissionCheckResult nameWriteResult2 = permissionAuthService.checkPermission(
                PermissionRequest.column(
                        normalUser.getId(), normalUser.getEmail(), testConnection.getId(),
                        PermissionType.WRITE,
                        "PUBLIC", "users", "name"
                )
        );
        assertFalse(nameWriteResult2.granted());
    }

    private void createPermission(UserEntity user, PermissionScope scope, PermissionType permissionType,
                                  String schemaName, String tableName, String columnName, boolean granted) {
        createPermissionWithExpiry(user, scope, permissionType, schemaName, tableName, columnName, granted, null);
    }

    private void createPermissionWithExpiry(UserEntity user, PermissionScope scope, PermissionType permissionType,
                                            String schemaName, String tableName, String columnName,
                                            boolean granted, LocalDateTime expiresAt) {
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

    private void setSecurityContext(UserEntity user) {
        UserDetails userDetails = User.builder()
                .username(user.getEmail())
                .password("password")
                .roles(user.getRole().name())
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

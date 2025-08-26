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

import cherry.mastermeister.controller.dto.PermissionExportData;
import cherry.mastermeister.entity.*;
import cherry.mastermeister.enums.*;
import cherry.mastermeister.repository.*;
import cherry.mastermeister.service.PermissionYamlService;
import cherry.mastermeister.service.PermissionYamlService.ImportOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
public class PermissionExportIntegrationTest {

    @Autowired
    private PermissionYamlService permissionYamlService;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseConnectionRepository databaseConnectionRepository;

    @Autowired
    private PermissionTemplateRepository permissionTemplateRepository;

    @Autowired
    private PermissionTemplateItemRepository permissionTemplateItemRepository;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());

    private UserEntity testUser1;
    private UserEntity testUser2;
    private UserEntity adminUser;
    private DatabaseConnectionEntity testConnection;

    @BeforeEach
    void setUp() {
        // Clear security context
        SecurityContextHolder.clearContext();

        // Create or find admin user
        adminUser = userRepository.findByEmail("admin@example.com").orElse(null);
        if (adminUser == null) {
            adminUser = new UserEntity();
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword("password123"); // Required field
            adminUser.setStatus(UserStatus.APPROVED);
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser = userRepository.save(adminUser);
        }

        // Set up security context with admin user
        setSecurityContext(adminUser);

        // Create test users
        testUser1 = new UserEntity();
        testUser1.setEmail("user1@example.com");
        testUser1.setPassword("password123"); // Required field
        testUser1.setStatus(UserStatus.APPROVED);
        testUser1.setRole(UserRole.USER);
        testUser1.setCreatedAt(LocalDateTime.now());
        testUser1 = userRepository.save(testUser1);

        testUser2 = new UserEntity();
        testUser2.setEmail("user2@example.com");
        testUser2.setPassword("password123"); // Required field
        testUser2.setStatus(UserStatus.APPROVED);
        testUser2.setRole(UserRole.USER);
        testUser2.setCreatedAt(LocalDateTime.now());
        testUser2 = userRepository.save(testUser2);

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
    }

    @Test
    void testExportBasicPermissions() throws Exception {
        // Setup test permissions
        createTestPermission(testUser1, PermissionScope.TABLE, PermissionType.READ,
                "PUBLIC", "users", null, true, null, "Basic read access");
        createTestPermission(testUser1, PermissionScope.COLUMN, PermissionType.WRITE,
                "PUBLIC", "users", "email", true, null, "Email write access");
        createTestPermission(testUser2, PermissionScope.SCHEMA, PermissionType.READ,
                "PUBLIC", null, null, true, null, "Schema read access");

        // Execute export
        String yamlContent = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Test export");

        // Parse and verify YAML structure
        PermissionExportData exportData = yamlMapper.readValue(yamlContent, PermissionExportData.class);

        // Verify export info
        assertNotNull(exportData.exportInfo());
        assertEquals("1.0", exportData.exportInfo().version());
        assertEquals("admin@example.com", exportData.exportInfo().exportedBy());
        assertNotNull(exportData.exportInfo().exportedAt());

        // Verify connection info
        assertNotNull(exportData.connectionInfo());
        assertEquals(testConnection.getId(), exportData.connectionInfo().connectionId());
        assertEquals("Test Database", exportData.connectionInfo().connectionName());
        assertEquals("H2", exportData.connectionInfo().databaseType());
        assertEquals("testdb", exportData.connectionInfo().databaseName());

        // Verify users and permissions
        assertNotNull(exportData.users());
        assertEquals(2, exportData.users().size());

        // Check user1 permissions
        var user1Data = exportData.users().stream()
                .filter(u -> u.userEmail().equals("user1@example.com"))
                .findFirst()
                .orElse(null);
        assertNotNull(user1Data);
        assertEquals(2, user1Data.permissions().size());

        // Check TABLE permission
        var tablePermission = user1Data.permissions().stream()
                .filter(p -> "TABLE".equals(p.scope()))
                .findFirst()
                .orElse(null);
        assertNotNull(tablePermission);
        assertEquals("READ", tablePermission.permissionType());
        assertEquals("PUBLIC", tablePermission.schemaName());
        assertEquals("users", tablePermission.tableName());
        assertNull(tablePermission.columnName());
        assertTrue(tablePermission.granted());
        assertEquals("Basic read access", tablePermission.comment());

        // Check COLUMN permission
        var columnPermission = user1Data.permissions().stream()
                .filter(p -> "COLUMN".equals(p.scope()))
                .findFirst()
                .orElse(null);
        assertNotNull(columnPermission);
        assertEquals("WRITE", columnPermission.permissionType());
        assertEquals("PUBLIC", columnPermission.schemaName());
        assertEquals("users", columnPermission.tableName());
        assertEquals("email", columnPermission.columnName());
        assertTrue(columnPermission.granted());

        // Check user2 permissions
        var user2Data = exportData.users().stream()
                .filter(u -> u.userEmail().equals("user2@example.com"))
                .findFirst()
                .orElse(null);
        assertNotNull(user2Data);
        assertEquals(1, user2Data.permissions().size());

        var schemaPermission = user2Data.permissions().get(0);
        assertEquals("SCHEMA", schemaPermission.scope());
        assertEquals("READ", schemaPermission.permissionType());
        assertEquals("PUBLIC", schemaPermission.schemaName());
        assertNull(schemaPermission.tableName());
        assertNull(schemaPermission.columnName());
    }

    @Test
    void testExportWithExpirationDates() throws Exception {
        LocalDateTime expireDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        // Setup permission with expiration
        createTestPermission(testUser1, PermissionScope.TABLE, PermissionType.READ,
                "PUBLIC", "temp_data", null, true, expireDate, "Temporary access");

        // Execute export
        String yamlContent = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Test export");

        // Parse and verify
        PermissionExportData exportData = yamlMapper.readValue(yamlContent, PermissionExportData.class);

        var userData = exportData.users().get(0);
        var permission = userData.permissions().get(0);

        assertEquals(expireDate, permission.expiresAt());
        assertEquals("Temporary access", permission.comment());
    }

    @Test
    void testExportWithDeniedPermissions() throws Exception {
        // Setup denied permission
        createTestPermission(testUser1, PermissionScope.TABLE, PermissionType.DELETE,
                "PUBLIC", "logs", null, false, null, "Delete denied on logs");

        // Execute export
        String yamlContent = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Test export");

        // Parse and verify
        PermissionExportData exportData = yamlMapper.readValue(yamlContent, PermissionExportData.class);

        assertNotNull(exportData.users());
        assertFalse(exportData.users().isEmpty());
        var userData = exportData.users().get(0);
        assertNotNull(userData.permissions());
        assertFalse(userData.permissions().isEmpty());
        var permission = userData.permissions().get(0);

        assertFalse(permission.granted());
        assertEquals("Delete denied on logs", permission.comment());
    }

    @Test
    void testExportWithTemplates() throws Exception {
        // Create permission template
        PermissionTemplateEntity template = new PermissionTemplateEntity();
        template.setName("Basic User Template");
        template.setDescription("Standard read-only access");
        template.setConnectionId(testConnection.getId()); // Required field
        template.setIsActive(true);
        template.setCreatedBy("admin@example.com");
        template.setCreatedAt(LocalDateTime.now());
        template = permissionTemplateRepository.save(template);

        // Create template items
        PermissionTemplateItemEntity templateItem1 = new PermissionTemplateItemEntity();
        templateItem1.setTemplate(template);
        templateItem1.setScope(PermissionScope.TABLE);
        templateItem1.setPermissionType(PermissionType.READ);
        templateItem1.setSchemaName("PUBLIC");
        templateItem1.setTableName("users");
        templateItem1.setGranted(true);
        templateItem1.setComment("Basic user read access");
        templateItem1 = permissionTemplateItemRepository.save(templateItem1);

        PermissionTemplateItemEntity templateItem2 = new PermissionTemplateItemEntity();
        templateItem2.setTemplate(template);
        templateItem2.setScope(PermissionScope.TABLE);
        templateItem2.setPermissionType(PermissionType.DELETE);
        templateItem2.setSchemaName("PUBLIC");
        templateItem2.setTableName("logs");
        templateItem2.setGranted(false);
        templateItem2.setComment("Deny log deletion");
        templateItem2 = permissionTemplateItemRepository.save(templateItem2);

        // Manually add items to template's collection to maintain bidirectional relationship
        template.getItems().add(templateItem1);
        template.getItems().add(templateItem2);
        template = permissionTemplateRepository.save(template);


        // Execute export
        String yamlContent = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Test export");

        // Parse and verify
        PermissionExportData exportData = yamlMapper.readValue(yamlContent, PermissionExportData.class);

        // Verify templates are included
        assertNotNull(exportData.templates());
        assertEquals(1, exportData.templates().size());

        var templateData = exportData.templates().get(0);
        assertEquals("Basic User Template", templateData.name());
        assertEquals("Standard read-only access", templateData.description());
        assertTrue(templateData.isActive());

        // Template items should be properly loaded with JOIN FETCH
        assertEquals(2, templateData.items().size());

        var readItem = templateData.items().stream()
                .filter(item -> "READ".equals(item.permissionType()))
                .findFirst()
                .orElse(null);
        assertNotNull(readItem);
        assertEquals("TABLE", readItem.scope());
        assertEquals("users", readItem.tableName());
        assertTrue(readItem.granted());

        var deleteItem = templateData.items().stream()
                .filter(item -> "DELETE".equals(item.permissionType()))
                .findFirst()
                .orElse(null);
        assertNotNull(deleteItem);
        assertEquals("TABLE", deleteItem.scope());
        assertEquals("logs", deleteItem.tableName());
        assertFalse(deleteItem.granted());
    }

    @Test
    void testExportWithNoPermissions() throws Exception {
        // Execute export with no permissions
        String yamlContent = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Test export");

        // Parse and verify
        PermissionExportData exportData = yamlMapper.readValue(yamlContent, PermissionExportData.class);

        // Should still have basic structure
        assertNotNull(exportData.exportInfo());
        assertNotNull(exportData.connectionInfo());

        // Users list should be empty or null
        assertTrue(exportData.users() == null || exportData.users().isEmpty());

        // Templates list should be empty or null
        assertTrue(exportData.templates() == null || exportData.templates().isEmpty());
    }

    @Test
    void testExportWithInvalidConnectionId() {
        // Test export with non-existent connection ID
        assertThrows(RuntimeException.class, () -> {
            permissionYamlService.exportPermissionsAsYaml(999999L, "Invalid connection test");
        });
    }

    @Test
    void testRoundTripImportExport() throws Exception {
        // Setup initial permissions
        createTestPermission(testUser1, PermissionScope.CONNECTION, PermissionType.READ,
                null, null, null, true, null, "Full connection read");
        createTestPermission(testUser2, PermissionScope.TABLE, PermissionType.WRITE,
                "PUBLIC", "products", null, true, LocalDateTime.of(2025, 6, 30, 12, 0), "Temporary write access");

        // Export permissions
        String originalYaml = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Round-trip test - original");

        // Clear existing permissions
        userPermissionRepository.deleteAll();

        // Import the exported YAML
        ImportOptions options = new ImportOptions(true, false, false, false);
        permissionYamlService.importPermissionsFromYaml(originalYaml, testConnection.getId(), options);

        // Export again
        String reimportedYaml = permissionYamlService.exportPermissionsAsYaml(testConnection.getId(), "Round-trip test - reimported");

        // Parse both YAMLs
        PermissionExportData originalData = yamlMapper.readValue(originalYaml, PermissionExportData.class);
        PermissionExportData reimportedData = yamlMapper.readValue(reimportedYaml, PermissionExportData.class);

        // Compare permissions (ignoring timestamp differences)
        assertEquals(originalData.users().size(), reimportedData.users().size());

        for (int i = 0; i < originalData.users().size(); i++) {
            var originalUser = originalData.users().get(i);
            var reimportedUser = reimportedData.users().stream()
                    .filter(u -> u.userEmail().equals(originalUser.userEmail()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(reimportedUser);
            assertEquals(originalUser.permissions().size(), reimportedUser.permissions().size());

            // Compare each permission
            for (var originalPerm : originalUser.permissions()) {
                var reimportedPerm = reimportedUser.permissions().stream()
                        .filter(p -> p.scope().equals(originalPerm.scope()) &&
                                p.permissionType().equals(originalPerm.permissionType()) &&
                                java.util.Objects.equals(p.schemaName(), originalPerm.schemaName()) &&
                                java.util.Objects.equals(p.tableName(), originalPerm.tableName()) &&
                                java.util.Objects.equals(p.columnName(), originalPerm.columnName()))
                        .findFirst()
                        .orElse(null);

                assertNotNull(reimportedPerm, "Permission not found after round-trip");
                assertEquals(originalPerm.granted(), reimportedPerm.granted());
                assertEquals(originalPerm.expiresAt(), reimportedPerm.expiresAt());
                assertEquals(originalPerm.comment(), reimportedPerm.comment());
            }
        }
    }

    private void createTestPermission(UserEntity user, PermissionScope scope, PermissionType permissionType,
                                      String schemaName, String tableName, String columnName,
                                      boolean granted, LocalDateTime expiresAt, String comment) {
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
        permission.setComment(comment);
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

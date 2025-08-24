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

package cherry.mastermeister.service;

import cherry.mastermeister.enums.DatabaseType;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.DatabaseConnection;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaReaderServiceTest {

    @Mock
    private DatabaseConnectionService databaseConnectionService;

    @Mock
    private SchemaMetadataStorageService schemaMetadataStorageService;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private ResultSet schemasResultSet;

    @Mock
    private ResultSet tablesResultSet;

    @Mock
    private ResultSet columnsResultSet;

    @Mock
    private ResultSet primaryKeysResultSet;

    private SchemaReaderService schemaReaderService;
    private DatabaseConnection dbConnection;

    @BeforeEach
    void setUp() {
        schemaReaderService = new SchemaReaderService(databaseConnectionService, schemaMetadataStorageService);

        dbConnection = new DatabaseConnection(
                1L, "Test Connection", DatabaseType.H2, "mem", 9092,
                "testdb", "sa", "", null, true,
                LocalDateTime.now(), true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void testReadSchemaForH2() throws SQLException {
        // Setup mocks
        when(databaseConnectionService.getConnection(1L)).thenReturn(dbConnection);
        when(databaseConnectionService.getDataSource(1L)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);

        // Mock schemas
        when(metaData.getSchemas()).thenReturn(schemasResultSet);
        when(schemasResultSet.next()).thenReturn(true, false);
        when(schemasResultSet.getString("TABLE_SCHEM")).thenReturn("PUBLIC");

        // Mock tables
        when(metaData.getTables("testdb", "PUBLIC", null, new String[]{"TABLE", "VIEW"}))
                .thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(true, false);
        when(tablesResultSet.getString("TABLE_NAME")).thenReturn("TEST_TABLE");
        when(tablesResultSet.getString("TABLE_TYPE")).thenReturn("TABLE");
        when(tablesResultSet.getString("REMARKS")).thenReturn("Test table");

        // Mock primary keys
        when(metaData.getPrimaryKeys("testdb", "PUBLIC", "TEST_TABLE"))
                .thenReturn(primaryKeysResultSet);
        when(primaryKeysResultSet.next()).thenReturn(true, false);
        when(primaryKeysResultSet.getString("COLUMN_NAME")).thenReturn("ID");

        // Mock columns
        when(metaData.getColumns("testdb", "PUBLIC", "TEST_TABLE", null))
                .thenReturn(columnsResultSet);
        when(columnsResultSet.next()).thenReturn(true, true, false);

        // First column (ID)
        when(columnsResultSet.getString("COLUMN_NAME"))
                .thenReturn("ID")
                .thenReturn("NAME");
        when(columnsResultSet.getString("TYPE_NAME"))
                .thenReturn("BIGINT")
                .thenReturn("VARCHAR");
        when(columnsResultSet.getInt("COLUMN_SIZE"))
                .thenReturn(19)
                .thenReturn(255);
        when(columnsResultSet.wasNull()).thenReturn(false, false);
        when(columnsResultSet.getInt("DECIMAL_DIGITS")).thenReturn(0, 0);
        when(columnsResultSet.getInt("NULLABLE"))
                .thenReturn(DatabaseMetaData.columnNoNulls)
                .thenReturn(DatabaseMetaData.columnNullable);
        when(columnsResultSet.getString("COLUMN_DEF")).thenReturn(null);
        when(columnsResultSet.getString("REMARKS")).thenReturn("ID column", "Name column");
        when(columnsResultSet.getString("IS_AUTOINCREMENT")).thenReturn("YES", "NO");
        when(columnsResultSet.getInt("ORDINAL_POSITION")).thenReturn(1, 2);

        // Mock storage service to return the saved metadata
        when(schemaMetadataStorageService.saveSchemaMetadata(any(SchemaMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        SchemaMetadata result = schemaReaderService.readSchema(1L);

        // Verify
        assertNotNull(result);
        assertEquals(1L, result.connectionId());
        assertEquals("testdb", result.databaseName());
        assertEquals(List.of("PUBLIC"), result.schemas());
        assertEquals(1, result.tables().size());

        TableMetadata table = result.tables().get(0);
        assertEquals("PUBLIC", table.schema());
        assertEquals("TEST_TABLE", table.tableName());
        assertEquals("TABLE", table.tableType());
        assertEquals("Test table", table.comment());
        assertEquals(2, table.columns().size());

        ColumnMetadata idColumn = table.columns().get(0);
        assertEquals("ID", idColumn.columnName());
        assertEquals("BIGINT", idColumn.dataType());
        assertTrue(idColumn.primaryKey());
        assertTrue(idColumn.autoIncrement());
        assertFalse(idColumn.nullable());
        assertEquals(1, idColumn.ordinalPosition());

        ColumnMetadata nameColumn = table.columns().get(1);
        assertEquals("NAME", nameColumn.columnName());
        assertEquals("VARCHAR", nameColumn.dataType());
        assertFalse(nameColumn.primaryKey());
        assertFalse(nameColumn.autoIncrement());
        assertTrue(nameColumn.nullable());
        assertEquals(2, nameColumn.ordinalPosition());

        // Verify storage service was called
        verify(schemaMetadataStorageService).saveSchemaMetadata(any(SchemaMetadata.class));
    }

    @Test
    void testReadSchemaThrowsExceptionOnSQLError() throws SQLException {
        when(databaseConnectionService.getConnection(1L)).thenReturn(dbConnection);
        when(databaseConnectionService.getDataSource(1L)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        assertThrows(RuntimeException.class, () -> schemaReaderService.readSchema(1L));
    }
}

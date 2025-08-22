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

import cherry.mastermeister.entity.DatabaseConnection;
import cherry.mastermeister.entity.DatabaseConnection.DatabaseType;
import cherry.mastermeister.repository.DatabaseConnectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConnectionServiceTest {

    @Mock
    private DatabaseConnectionRepository repository;

    @Test
    void testGetDataSource_CreateNewDataSource() {
        DatabaseConnectionService service = new DatabaseConnectionService(repository);
        DatabaseConnection dbConnection = createTestDatabaseConnection();
        dbConnection.setDbType(DatabaseType.H2);
        dbConnection.setHost("mem");
        dbConnection.setDatabaseName("testdb");
        dbConnection.setConnectionParams("DB_CLOSE_DELAY=-1");
        when(repository.findById(1L)).thenReturn(Optional.of(dbConnection));

        DataSource dataSource = service.getDataSource(1L);

        assertNotNull(dataSource);
        verify(repository).findById(1L);
    }

    @Test
    void testGetDataSource_ThrowsExceptionWhenConnectionNotFound() {
        DatabaseConnectionService service = new DatabaseConnectionService(repository);
        when(repository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getDataSource(1L)
        );

        assertEquals("Database connection not found: 1", exception.getMessage());
    }

    @Test
    void testGetDataSource_ThrowsExceptionWhenConnectionInactive() {
        DatabaseConnectionService service = new DatabaseConnectionService(repository);
        DatabaseConnection dbConnection = createTestDatabaseConnection();
        dbConnection.setActive(false);
        when(repository.findById(1L)).thenReturn(Optional.of(dbConnection));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.getDataSource(1L)
        );

        assertEquals("Database connection is not active: 1", exception.getMessage());
    }

    @Test
    void testTestConnection_ConnectionNotFound() {
        DatabaseConnectionService service = new DatabaseConnectionService(repository);
        when(repository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.testConnection(1L)
        );

        assertEquals("Database connection not found: 1", exception.getMessage());
    }

    @Test
    void testCloseDataSource() {
        DatabaseConnectionService service = new DatabaseConnectionService(repository);
        DatabaseConnection dbConnection = createTestDatabaseConnection();
        dbConnection.setDbType(DatabaseType.H2);
        dbConnection.setHost("mem");
        dbConnection.setDatabaseName("testdb");
        dbConnection.setConnectionParams("DB_CLOSE_DELAY=-1");
        when(repository.findById(1L)).thenReturn(Optional.of(dbConnection));

        service.getDataSource(1L);
        service.closeDataSource(1L);

        verify(repository).findById(1L);
    }

    @Test
    void testCloseAllDataSources() {
        DatabaseConnectionService service = new DatabaseConnectionService(repository);
        DatabaseConnection dbConnection1 = createTestDatabaseConnection();
        dbConnection1.setDbType(DatabaseType.H2);
        dbConnection1.setHost("mem");
        dbConnection1.setDatabaseName("testdb1");
        dbConnection1.setConnectionParams("DB_CLOSE_DELAY=-1");
        
        DatabaseConnection dbConnection2 = createTestDatabaseConnection();
        dbConnection2.setId(2L);
        dbConnection2.setDbType(DatabaseType.H2);
        dbConnection2.setHost("mem");
        dbConnection2.setDatabaseName("testdb2");
        dbConnection2.setConnectionParams("DB_CLOSE_DELAY=-1");
        
        when(repository.findById(1L)).thenReturn(Optional.of(dbConnection1));
        when(repository.findById(2L)).thenReturn(Optional.of(dbConnection2));

        service.getDataSource(1L);
        service.getDataSource(2L);
        service.closeAllDataSources();

        verify(repository).findById(1L);
        verify(repository).findById(2L);
    }

    private DatabaseConnection createTestDatabaseConnection() {
        DatabaseConnection dbConnection = new DatabaseConnection();
        dbConnection.setId(1L);
        dbConnection.setName("test-connection");
        dbConnection.setDbType(DatabaseType.H2);
        dbConnection.setHost("localhost");
        dbConnection.setPort(9092);
        dbConnection.setDatabaseName("test");
        dbConnection.setUsername("sa");
        dbConnection.setPassword("");
        dbConnection.setActive(true);
        dbConnection.setCreatedAt(LocalDateTime.now());
        dbConnection.setUpdatedAt(LocalDateTime.now());
        return dbConnection;
    }
}
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

import cherry.mastermeister.entity.SchemaMetadataEntity;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.repository.SchemaMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaMetadataServiceTest {

    @Mock
    private SchemaMetadataRepository schemaMetadataRepository;

    private SchemaMetadataService service;

    @BeforeEach
    void setUp() {
        service = new SchemaMetadataService(
                schemaMetadataRepository
        );
    }

    @Test
    void testSaveSchemaMetadata() {
        // Setup test data
        SchemaMetadataEntity toBeDeleted = mock(SchemaMetadataEntity.class);
        when(schemaMetadataRepository.findByConnectionId(1L)).thenReturn(Optional.of(toBeDeleted));

        ColumnMetadata column = new ColumnMetadata(
                "ID", "BIGINT", 19, null, false, null, "ID column",
                true, true, 1
        );

        TableMetadata table = new TableMetadata(
                "PUBLIC", "TEST_TABLE", "TABLE", "Test table", List.of(column)
        );

        SchemaMetadata metadata = new SchemaMetadata(
                1L, "testdb", List.of("PUBLIC"), List.of(table), LocalDateTime.now()
        );

        SchemaMetadataEntity savedEntity = new SchemaMetadataEntity();
        savedEntity.setId(1L);
        savedEntity.setConnectionId(1L);
        savedEntity.setDatabaseName("testdb");
        savedEntity.setSchemas(List.of("PUBLIC"));
        savedEntity.setTables(List.of());
        savedEntity.setLastUpdatedAt(LocalDateTime.now());

        when(schemaMetadataRepository.save(any(SchemaMetadataEntity.class))).thenReturn(savedEntity);

        // Execute
        SchemaMetadata result = service.saveSchemaMetadata(metadata);

        // Verify
        assertNotNull(result);
        assertEquals(1L, result.connectionId());
        assertEquals("testdb", result.databaseName());

        verify(schemaMetadataRepository).findByConnectionId(1L);
        verify(schemaMetadataRepository).delete(eq(toBeDeleted));
        verify(schemaMetadataRepository).save(any(SchemaMetadataEntity.class));
    }

    @Test
    void testGetSchemaMetadata() {
        SchemaMetadataEntity entity = new SchemaMetadataEntity();
        entity.setId(1L);
        entity.setConnectionId(1L);
        entity.setDatabaseName("testdb");
        entity.setSchemas(List.of("PUBLIC"));
        entity.setTables(List.of());
        entity.setLastUpdatedAt(LocalDateTime.now());

        when(schemaMetadataRepository.findByConnectionId(1L)).thenReturn(Optional.of(entity));

        Optional<SchemaMetadata> result = service.getSchemaMetadata(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().connectionId());
        assertEquals("testdb", result.get().databaseName());
    }

    @Test
    void testGetSchemaMetadataNotFound() {
        when(schemaMetadataRepository.findByConnectionId(1L)).thenReturn(Optional.empty());

        Optional<SchemaMetadata> result = service.getSchemaMetadata(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void testDeleteSchemaMetadata() {
        // Setup test data
        SchemaMetadataEntity toBeDeleted = mock(SchemaMetadataEntity.class);
        when(schemaMetadataRepository.findByConnectionId(1L)).thenReturn(Optional.of(toBeDeleted));

        service.deleteSchemaMetadata(1L);

        verify(schemaMetadataRepository).findByConnectionId(1L);
        verify(schemaMetadataRepository).delete(eq(toBeDeleted));
    }

    @Test
    void testExistsSchemaMetadata() {
        when(schemaMetadataRepository.existsByConnectionId(1L)).thenReturn(true);

        boolean exists = service.existsSchemaMetadata(1L);

        assertTrue(exists);
        verify(schemaMetadataRepository).existsByConnectionId(1L);
    }
}

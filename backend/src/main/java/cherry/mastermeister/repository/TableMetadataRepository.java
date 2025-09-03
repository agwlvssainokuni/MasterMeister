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

package cherry.mastermeister.repository;

import cherry.mastermeister.entity.TableMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadataEntity, Long> {

    /**
     * Find table metadata by connection, schema and table name
     */
    Optional<TableMetadataEntity> findBySchemaMetadata_ConnectionIdAndSchemaAndTableName(
            Long connectionId,
            String schema, String tableName
    );

    /**
     * Find all tables for a specific connection and schema
     */
    List<TableMetadataEntity> findBySchemaMetadata_ConnectionIdAndSchema(
            Long connectionId,
            String schema
    );

    /**
     * Find all tables for a specific connection
     */
    List<TableMetadataEntity> findBySchemaMetadata_ConnectionId(
            Long connectionId
    );

    /**
     * Get all column names for a specific table
     */
    @Query("""
            SELECT c.columnName FROM TableMetadataEntity t
            JOIN t.columns c
            WHERE t.schemaMetadata.connectionId = :connectionId
                AND t.schema = :schema
                AND t.tableName = :tableName
            ORDER BY c.ordinalPosition
            """)
    List<String> findColumnNamesByTable(
            Long connectionId, String schema, String tableName
    );
}

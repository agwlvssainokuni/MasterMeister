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

import cherry.mastermeister.entity.ColumnMetadataEntity;
import cherry.mastermeister.entity.SchemaMetadataEntity;
import cherry.mastermeister.entity.TableMetadataEntity;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.repository.SchemaMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SchemaMetadataStorageService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SchemaMetadataRepository schemaMetadataRepository;

    public SchemaMetadataStorageService(SchemaMetadataRepository schemaMetadataRepository) {
        this.schemaMetadataRepository = schemaMetadataRepository;
    }

    @Transactional
    public SchemaMetadata saveSchemaMetadata(SchemaMetadata metadata) {
        logger.info("Saving schema metadata for connection ID: {}", metadata.connectionId());

        // Delete existing metadata for this connection
        schemaMetadataRepository.deleteByConnectionId(metadata.connectionId());

        // Create new entity
        SchemaMetadataEntity entity = toEntity(metadata);
        SchemaMetadataEntity saved = schemaMetadataRepository.save(entity);

        logger.info("Saved schema metadata with ID: {} for connection ID: {}",
                saved.getId(), metadata.connectionId());

        return toModel(saved);
    }

    @Transactional(readOnly = true)
    public Optional<SchemaMetadata> getSchemaMetadata(Long connectionId) {
        logger.debug("Retrieving schema metadata for connection ID: {}", connectionId);

        return schemaMetadataRepository.findByConnectionId(connectionId)
                .map(this::toModel);
    }

    @Transactional
    public void deleteSchemaMetadata(Long connectionId) {
        logger.info("Deleting schema metadata for connection ID: {}", connectionId);
        schemaMetadataRepository.deleteByConnectionId(connectionId);
    }

    @Transactional(readOnly = true)
    public boolean existsSchemaMetadata(Long connectionId) {
        return schemaMetadataRepository.existsByConnectionId(connectionId);
    }

    @Transactional(readOnly = true)
    public List<SchemaMetadata> getAllSchemaMetadata() {
        logger.debug("Retrieving all schema metadata");

        return schemaMetadataRepository.findAll().stream()
                .map(this::toModel)
                .toList();
    }

    private SchemaMetadataEntity toEntity(SchemaMetadata model) {
        SchemaMetadataEntity entity = new SchemaMetadataEntity();
        entity.setConnectionId(model.connectionId());
        entity.setDatabaseName(model.databaseName());
        entity.setSchemas(model.schemas());
        entity.setLastUpdatedAt(model.lastUpdatedAt());

        List<TableMetadataEntity> tableEntities = model.tables().stream()
                .map(table -> toEntity(table, entity))
                .toList();
        entity.setTables(tableEntities);

        return entity;
    }

    private TableMetadataEntity toEntity(TableMetadata model, SchemaMetadataEntity schemaEntity) {
        TableMetadataEntity entity = new TableMetadataEntity();
        entity.setSchema(model.schema());
        entity.setTableName(model.tableName());
        entity.setTableType(model.tableType());
        entity.setComment(model.comment());
        entity.setSchemaMetadata(schemaEntity);

        List<ColumnMetadataEntity> columnEntities = model.columns().stream()
                .map(column -> toEntity(column, entity))
                .toList();
        entity.setColumns(columnEntities);

        return entity;
    }

    private ColumnMetadataEntity toEntity(ColumnMetadata model, TableMetadataEntity tableEntity) {
        ColumnMetadataEntity entity = new ColumnMetadataEntity();
        entity.setColumnName(model.columnName());
        entity.setDataType(model.dataType());
        entity.setColumnSize(model.columnSize());
        entity.setDecimalDigits(model.decimalDigits());
        entity.setNullable(model.nullable());
        entity.setDefaultValue(model.defaultValue());
        entity.setComment(model.comment());
        entity.setPrimaryKey(model.primaryKey());
        entity.setAutoIncrement(model.autoIncrement());
        entity.setOrdinalPosition(model.ordinalPosition());
        entity.setTableMetadata(tableEntity);

        return entity;
    }

    private SchemaMetadata toModel(SchemaMetadataEntity entity) {
        List<TableMetadata> tables = entity.getTables().stream()
                .map(this::toModel)
                .toList();

        return new SchemaMetadata(
                entity.getConnectionId(),
                entity.getDatabaseName(),
                entity.getSchemas(),
                tables,
                entity.getLastUpdatedAt()
        );
    }

    private TableMetadata toModel(TableMetadataEntity entity) {
        List<ColumnMetadata> columns = entity.getColumns().stream()
                .map(this::toModel)
                .toList();

        return new TableMetadata(
                entity.getSchema(),
                entity.getTableName(),
                entity.getTableType(),
                entity.getComment(),
                columns
        );
    }

    private ColumnMetadata toModel(ColumnMetadataEntity entity) {
        return new ColumnMetadata(
                entity.getColumnName(),
                entity.getDataType(),
                entity.getColumnSize(),
                entity.getDecimalDigits(),
                entity.getNullable(),
                entity.getDefaultValue(),
                entity.getComment(),
                entity.getPrimaryKey(),
                entity.getAutoIncrement(),
                entity.getOrdinalPosition()
        );
    }
}

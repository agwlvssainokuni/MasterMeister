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

package cherry.mastermeister.entity;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

@Entity
@Table(name = "table_metadata")
public class TableMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schema;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "table_type", nullable = false, length = 20)
    private String tableType;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_metadata_id", nullable = false)
    private SchemaMetadataEntity schemaMetadata;

    @OneToMany(mappedBy = "tableMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinalPosition ASC")
    private List<ColumnMetadataEntity> columns;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public SchemaMetadataEntity getSchemaMetadata() {
        return schemaMetadata;
    }

    public void setSchemaMetadata(SchemaMetadataEntity schemaMetadata) {
        this.schemaMetadata = schemaMetadata;
    }

    public List<ColumnMetadataEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMetadataEntity> columns) {
        this.columns = columns;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TableMetadataEntity that = (TableMetadataEntity) obj;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(schema, that.schema)
                .append(tableName, that.tableName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(schema)
                .append(tableName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("schema", schema)
                .append("tableName", tableName)
                .append("tableType", tableType)
                .toString();
    }
}

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

import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.PermissionType;
import jakarta.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "permission_template_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "template_id", "scope", "schema_name", "table_name", "column_name", "permission_type"
        }),
        indexes = {
                @Index(name = "idx_perm_template_items_template", columnList = "template_id"),
                @Index(name = "idx_perm_template_items_scope", columnList = "scope")
        })
public class PermissionTemplateItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_perm_template_item_template"))
    private PermissionTemplateEntity template;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PermissionScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 20)
    private PermissionType permissionType;

    @Column(name = "schema_name", length = 100)
    private String schemaName;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "column_name", length = 100)
    private String columnName;

    @Column(name = "granted", nullable = false)
    private Boolean granted = true;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PermissionTemplateEntity getTemplate() {
        return template;
    }

    public void setTemplate(PermissionTemplateEntity template) {
        this.template = template;
    }

    public PermissionScope getScope() {
        return scope;
    }

    public void setScope(PermissionScope scope) {
        this.scope = scope;
    }

    public PermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PermissionTemplateItemEntity that = (PermissionTemplateItemEntity) obj;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(template, that.template)
                .append(scope, that.scope)
                .append(permissionType, that.permissionType)
                .append(schemaName, that.schemaName)
                .append(tableName, that.tableName)
                .append(columnName, that.columnName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(template)
                .append(scope)
                .append(permissionType)
                .append(schemaName)
                .append(tableName)
                .append(columnName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("scope", scope)
                .append("permissionType", permissionType)
                .append("schemaName", schemaName)
                .append("tableName", tableName)
                .append("columnName", columnName)
                .append("granted", granted)
                .toString();
    }
}

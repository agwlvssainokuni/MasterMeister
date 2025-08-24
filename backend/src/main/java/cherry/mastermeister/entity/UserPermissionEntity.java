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

import java.time.LocalDateTime;

@Entity
@Table(name = "user_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "user_id", "connection_id", "scope", "schema_name", "table_name", "column_name", "permission_type"
       }),
       indexes = {
           @Index(name = "idx_user_permissions_user", columnList = "user_id"),
           @Index(name = "idx_user_permissions_connection", columnList = "connection_id"),
           @Index(name = "idx_user_permissions_scope", columnList = "scope"),
           @Index(name = "idx_user_permissions_lookup", columnList = "user_id,connection_id,scope")
       })
public class UserPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_permission_user"))
    private UserEntity user;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

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

    @Column(name = "granted_by", length = 100)
    private String grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
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

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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
        UserPermissionEntity that = (UserPermissionEntity) obj;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(user, that.user)
                .append(connectionId, that.connectionId)
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
                .append(user)
                .append(connectionId)
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
                .append("connectionId", connectionId)
                .append("scope", scope)
                .append("permissionType", permissionType)
                .append("schemaName", schemaName)
                .append("tableName", tableName)
                .append("columnName", columnName)
                .append("granted", granted)
                .toString();
    }
}
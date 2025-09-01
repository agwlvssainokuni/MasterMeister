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

import cherry.mastermeister.entity.UserPermissionEntity;
import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermissionEntity, Long> {

    List<UserPermissionEntity> findByUserIdAndConnectionIdOrderByScopeAscSchemaNameAscTableNameAscColumnNameAsc(
            Long userId, Long connectionId
    );

    Optional<UserPermissionEntity> findByUserIdAndConnectionIdAndScopeAndPermissionTypeAndSchemaNameAndTableNameAndColumnName(
            Long userId, Long connectionId, PermissionScope scope,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName
    );

    @Query("""
            SELECT p FROM UserPermissionEntity p
            WHERE
                p.user.id = :userId AND p.connectionId = :connectionId
                AND p.scope = :scope AND p.permissionType = :permissionType
                AND ((:schemaName IS NULL AND p.schemaName IS NULL) OR p.schemaName = :schemaName)
                AND ((:tableName  IS NULL AND p.tableName  IS NULL) OR p.tableName  = :tableName)
                AND ((:columnName IS NULL AND p.columnName IS NULL) OR p.columnName = :columnName)
                AND p.grantedAt <= CURRENT_TIMESTAMP
                AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
            """)
    Optional<UserPermissionEntity> findActivePermission(
            Long userId, Long connectionId, PermissionScope scope,
            PermissionType permissionType,
            String schemaName, String tableName, String columnName
    );

    @Query("""
            SELECT p FROM UserPermissionEntity p
            WHERE
                p.user.id = :userId AND p.connectionId = :connectionId
                AND p.grantedAt <= CURRENT_TIMESTAMP
                AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
                ORDER BY p.scope ASC, p.schemaName ASC, p.tableName ASC, p.columnName ASC
            """)
    List<UserPermissionEntity> findActivePermissionsByUser(
            Long userId, Long connectionId
    );

    @Modifying
    @Query("""
            DELETE FROM UserPermissionEntity p
            WHERE
                p.connectionId = :connectionId
            """)
    int deleteByConnectionId(Long connectionId);
}

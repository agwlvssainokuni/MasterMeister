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

import cherry.mastermeister.entity.PermissionTemplateItemEntity;
import cherry.mastermeister.enums.PermissionScope;
import cherry.mastermeister.enums.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionTemplateItemRepository extends JpaRepository<PermissionTemplateItemEntity, Long> {

    List<PermissionTemplateItemEntity> findByTemplateIdOrderByScopeAscSchemaNameAscTableNameAscColumnNameAsc(Long templateId);

    List<PermissionTemplateItemEntity> findByTemplateIdAndScope(Long templateId, PermissionScope scope);

    @Query("SELECT i FROM PermissionTemplateItemEntity i WHERE i.template.id = :templateId " +
            "AND i.scope = :scope AND i.permissionType = :permissionType " +
            "AND (:schemaName IS NULL OR i.schemaName = :schemaName) " +
            "AND (:tableName IS NULL OR i.tableName = :tableName) " +
            "AND (:columnName IS NULL OR i.columnName = :columnName)")
    List<PermissionTemplateItemEntity> findByTemplateAndScopeAndType(
            Long templateId, PermissionScope scope, PermissionType permissionType,
            String schemaName, String tableName, String columnName);

    @Modifying
    @Query("DELETE FROM PermissionTemplateItemEntity i WHERE i.template.id = :templateId")
    int deleteByTemplateId(Long templateId);

    @Query("SELECT COUNT(i) FROM PermissionTemplateItemEntity i WHERE i.template.id = :templateId AND i.granted = true")
    long countGrantedItemsByTemplate(Long templateId);

    @Query("SELECT COUNT(i) FROM PermissionTemplateItemEntity i WHERE i.template.id = :templateId AND i.granted = false")
    long countDeniedItemsByTemplate(Long templateId);
}

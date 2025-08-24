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

import cherry.mastermeister.entity.PermissionTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionTemplateRepository extends JpaRepository<PermissionTemplateEntity, Long> {

    List<PermissionTemplateEntity> findByConnectionIdAndIsActiveTrueOrderByNameAsc(Long connectionId);

    List<PermissionTemplateEntity> findByConnectionIdOrderByCreatedAtDesc(Long connectionId);

    Optional<PermissionTemplateEntity> findByNameAndConnectionId(String name, Long connectionId);

    @Query("SELECT t FROM PermissionTemplateEntity t WHERE t.createdBy = :createdBy AND t.isActive = true ORDER BY t.createdAt DESC")
    List<PermissionTemplateEntity> findActiveTemplatesByCreator(String createdBy);

    @Modifying
    @Query("UPDATE PermissionTemplateEntity t SET t.isActive = false WHERE t.connectionId = :connectionId")
    int deactivateByConnectionId(Long connectionId);

    @Modifying
    @Query("DELETE FROM PermissionTemplateEntity t WHERE t.connectionId = :connectionId")
    int deleteByConnectionId(Long connectionId);

    boolean existsByNameAndConnectionIdAndIsActiveTrue(String name, Long connectionId);
}

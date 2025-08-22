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

import cherry.mastermeister.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByUsername(String username, Pageable pageable);

    Page<AuditLogEntity> findByAction(String action, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.timestamp BETWEEN :startTime AND :endTime")
    Page<AuditLogEntity> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    List<AuditLogEntity> findByUsernameAndTimestampBetween(String username, LocalDateTime startTime, LocalDateTime endTime);
}

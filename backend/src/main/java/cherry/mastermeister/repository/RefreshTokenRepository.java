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

import cherry.mastermeister.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenId(String tokenId);

    List<RefreshTokenEntity> findByUsername(String username);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.username = :username")
    int deleteAllByUsername(String username);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.tokenId = :tokenId")
    int deleteByTokenId(String tokenId);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :cutoffTime")
    int deleteExpiredTokens(LocalDateTime cutoffTime);

    @Query("SELECT COUNT(rt) FROM RefreshTokenEntity rt WHERE rt.username = :username")
    long countTokensByUsername(String username);
}

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

import cherry.mastermeister.entity.RefreshTokenEntity;
import cherry.mastermeister.model.TokenPair;
import cherry.mastermeister.repository.RefreshTokenRepository;
import cherry.mastermeister.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final int maxUsageCount;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtUtil jwtUtil,
                               @Value("${mm.security.jwt.refresh-token.max-usage-count:10}") int maxUsageCount) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.maxUsageCount = maxUsageCount;
    }

    public String createRefreshToken(UserDetails userDetails) {
        String tokenId = UUID.randomUUID().toString();
        String token = jwtUtil.generateRefreshToken(userDetails, tokenId);

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setTokenId(tokenId);
        refreshToken.setUsername(userDetails.getUsername());
        refreshToken.setExpiresAt(jwtUtil.extractExpirationAsLocalDateTime(token));

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        if (!jwtUtil.isRefreshToken(token)) {
            return false;
        }

        String tokenId = jwtUtil.extractTokenId(token);
        if (tokenId == null) {
            return false;
        }

        Optional<RefreshTokenEntity> refreshTokenOpt = refreshTokenRepository.findByTokenId(tokenId);
        if (refreshTokenOpt.isEmpty()) {
            return false;
        }

        RefreshTokenEntity refreshToken = refreshTokenOpt.get();

        // トークンの有効期限とユーザー名をチェック
        if (refreshToken.isExpired() || !refreshToken.getUsername().equals(userDetails.getUsername())) {
            return false;
        }

        // 使用回数制限をチェック
        if (refreshToken.hasExceededUsageLimit(maxUsageCount)) {
            return false;
        }

        // JWT自体の妥当性もチェック
        return jwtUtil.validateToken(token, userDetails);
    }

    public TokenPair refreshTokens(String refreshTokenString, UserDetails userDetails) {
        if (!validateRefreshToken(refreshTokenString, userDetails)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String oldTokenId = jwtUtil.extractTokenId(refreshTokenString);
        RefreshTokenEntity oldRefreshToken = refreshTokenRepository.findByTokenId(oldTokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // 古いリフレッシュトークンの使用回数をインクリメント（使用回数制限対応）
        oldRefreshToken.markAsUsed();
        refreshTokenRepository.save(oldRefreshToken);

        // 新しいトークンペアを生成
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);
        String newRefreshToken = createRefreshToken(userDetails);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public void revokeRefreshToken(String tokenId) {
        refreshTokenRepository.deleteByTokenId(tokenId);
    }

    public void revokeAllUserRefreshTokens(String username) {
        refreshTokenRepository.deleteAllByUsername(username);
    }

    public long countTokensForUser(String username) {
        return refreshTokenRepository.countTokensByUsername(username);
    }

    @Scheduled(fixedRate = 3600000) // 1時間ごと
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffTime = LocalDateTime.now();
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(cutoffTime);
        if (deletedCount > 0) {
            // ログ出力など（必要に応じて）
        }
    }
}

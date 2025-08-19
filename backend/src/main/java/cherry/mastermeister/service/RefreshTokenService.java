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

import cherry.mastermeister.entity.RefreshToken;
import cherry.mastermeister.model.TokenPair;
import cherry.mastermeister.repository.RefreshTokenRepository;
import cherry.mastermeister.util.JwtUtil;
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

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    public String createRefreshToken(UserDetails userDetails) {
        String tokenId = UUID.randomUUID().toString();
        String token = jwtUtil.generateRefreshToken(userDetails, tokenId);

        RefreshToken refreshToken = new RefreshToken();
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

        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenIdAndActiveTrue(tokenId);
        if (refreshTokenOpt.isEmpty()) {
            return false;
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        // トークンの有効期限とユーザー名をチェック
        if (refreshToken.isExpired() || !refreshToken.getUsername().equals(userDetails.getUsername())) {
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
        RefreshToken oldRefreshToken = refreshTokenRepository.findByTokenIdAndActiveTrue(oldTokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // 古いリフレッシュトークンを無効化 (Token Rotation)
        oldRefreshToken.deactivate();
        refreshTokenRepository.save(oldRefreshToken);

        // 新しいトークンペアを生成
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);
        String newRefreshToken = createRefreshToken(userDetails);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public void revokeRefreshToken(String tokenId) {
        refreshTokenRepository.deactivateByTokenId(tokenId);
    }

    public void revokeAllUserRefreshTokens(String username) {
        refreshTokenRepository.deactivateAllByUsername(username);
    }

    public long countActiveTokensForUser(String username) {
        return refreshTokenRepository.countActiveTokensByUsername(username);
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

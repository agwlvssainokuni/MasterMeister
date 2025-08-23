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

package cherry.mastermeister.util;

import cherry.mastermeister.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final String secret;
    private final Long accessTokenExpiration;
    private final Long refreshTokenExpiration;
    private final Boolean extendRefreshTokenOnUse;

    public JwtUtil(
            @Value("${mm.security.jwt.secret}") String secret,
            @Value("${mm.security.jwt.access-token.expiration:300}") Long accessTokenExpiration,
            @Value("${mm.security.jwt.refresh-token.expiration:86400}") Long refreshTokenExpiration,
            @Value("${mm.security.jwt.refresh-token.extend-on-use:true}") Boolean extendRefreshTokenOnUse
    ) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.extendRefreshTokenOnUse = extendRefreshTokenOnUse;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UserDetails userDetails) {
        // Extract all roles from authorities and remove ROLE_ prefix
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .toList();

        Map<String, Object> claims = Map.of(
                "type", "access",
                "role", roles,
                "email", userDetails.getUsername() // username is actually email
        );
        String subject = userDetails instanceof CustomUserDetails ?
                ((CustomUserDetails) userDetails).getUserUuid() :
                userDetails.getUsername();
        return createToken(claims, subject, accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails, String tokenId) {
        Map<String, Object> claims = Map.of(
                "type", "refresh",
                "jti", tokenId
        );
        String subject = userDetails instanceof CustomUserDetails ?
                ((CustomUserDetails) userDetails).getUserUuid() :
                userDetails.getUsername();
        return createToken(claims, subject, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationTime)))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUserUuid(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public LocalDateTime extractExpirationAsLocalDateTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String userUuid = extractUserUuid(token);
        if (userDetails instanceof CustomUserDetails) {
            return (userUuid.equals(((CustomUserDetails) userDetails).getUserUuid()) && !isTokenExpired(token));
        } else {
            // Fallback for non-custom UserDetails (shouldn't happen in normal flow)
            return (userUuid.equals(userDetails.getUsername()) && !isTokenExpired(token));
        }
    }

    public Boolean isRefreshToken(String token) {
        String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
        return "refresh".equals(tokenType);
    }

    public Boolean isAccessToken(String token) {
        String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
        return "access".equals(tokenType);
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public Boolean shouldExtendRefreshTokenOnUse() {
        return extendRefreshTokenOnUse;
    }
}

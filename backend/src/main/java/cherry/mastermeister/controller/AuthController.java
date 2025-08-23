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

package cherry.mastermeister.controller;

import cherry.mastermeister.controller.dto.*;
import cherry.mastermeister.model.TokenPair;
import cherry.mastermeister.service.AuditLogService;
import cherry.mastermeister.service.RefreshTokenService;
import cherry.mastermeister.service.UserDetailsServiceImpl;
import cherry.mastermeister.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsServiceImpl userDetailsService,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = refreshTokenService.createRefreshToken(userDetails);

            String role = userDetails.getAuthorities().iterator().next().getAuthority().substring(5);

            LoginResult result = new LoginResult(
                    accessToken,
                    refreshToken,
                    userDetails.getUsername(),
                    role,
                    jwtUtil.getAccessTokenExpiration()
            );

            // ログイン成功のログ記録
            auditLogService.logLoginSuccess(userDetails.getUsername(), httpRequest);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            // ログイン失敗のログ記録
            auditLogService.logLoginFailure(request.email(), e.getMessage(), httpRequest);

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(List.of("Invalid credentials")));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResult>> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        try {
            String refreshToken = request.refreshToken();

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(List.of("Invalid refresh token")));
            }

            String userUuid = jwtUtil.extractUserUuid(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUserUuid(userUuid);

            TokenPair tokenPair = refreshTokenService.refreshTokens(refreshToken, userDetails);
            String role = userDetails.getAuthorities().iterator().next().getAuthority().substring(5);

            LoginResult result = new LoginResult(
                    tokenPair.accessToken(),
                    tokenPair.refreshToken(),
                    userDetails.getUsername(),
                    role,
                    jwtUtil.getAccessTokenExpiration()
            );

            // トークンリフレッシュ成功のログ記録
            auditLogService.logTokenRefresh(userDetails.getUsername(), httpRequest);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            // トークンリフレッシュ失敗のログ記録
            String username = null;
            try {
                String userUuid = jwtUtil.extractUserUuid(request.refreshToken());
                username = userDetailsService.loadUserByUserUuid(userUuid).getUsername();
            } catch (Exception ignored) {
                // トークンが無効な場合はusernameを取得できない
            }
            auditLogService.logTokenRefreshFailure(username, e.getMessage(), httpRequest);

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(List.of("Failed to refresh token")));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logout user and revoke refresh token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully logged out"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid refresh token")
    })
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        try {
            String refreshToken = request.refreshToken();

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(List.of("Invalid refresh token")));
            }

            String tokenId = jwtUtil.extractTokenId(refreshToken);
            String userUuid = jwtUtil.extractUserUuid(refreshToken);
            String username = userDetailsService.loadUserByUserUuid(userUuid).getUsername();

            if (tokenId != null) {
                refreshTokenService.revokeRefreshToken(tokenId);
            }

            // ログアウト成功のログ記録
            auditLogService.logLogout(username, httpRequest);

            return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(List.of("Failed to logout")));
        }
    }
}

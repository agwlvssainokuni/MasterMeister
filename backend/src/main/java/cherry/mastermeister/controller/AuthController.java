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

import cherry.mastermeister.controller.dto.ApiResponse;
import cherry.mastermeister.controller.dto.LoginRequest;
import cherry.mastermeister.controller.dto.LoginResult;
import cherry.mastermeister.controller.dto.LogoutRequest;
import cherry.mastermeister.controller.dto.RefreshTokenRequest;
import cherry.mastermeister.model.TokenPair;
import cherry.mastermeister.service.RefreshTokenService;
import cherry.mastermeister.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
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

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(List.of("Invalid credentials")));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResult>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.refreshToken();

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(List.of("Invalid refresh token")));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            TokenPair tokenPair = refreshTokenService.refreshTokens(refreshToken, userDetails);
            String role = userDetails.getAuthorities().iterator().next().getAuthority().substring(5);

            LoginResult result = new LoginResult(
                    tokenPair.accessToken(),
                    tokenPair.refreshToken(),
                    userDetails.getUsername(),
                    role,
                    jwtUtil.getAccessTokenExpiration()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
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
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            String refreshToken = request.refreshToken();
            
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(List.of("Invalid refresh token")));
            }

            String tokenId = jwtUtil.extractTokenId(refreshToken);
            if (tokenId != null) {
                refreshTokenService.revokeRefreshToken(tokenId);
            }

            return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(List.of("Failed to logout")));
        }
    }
}

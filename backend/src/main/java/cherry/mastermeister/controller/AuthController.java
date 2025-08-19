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
import cherry.mastermeister.controller.dto.RefreshTokenRequest;
import cherry.mastermeister.util.JwtUtil;
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
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            String role = userDetails.getAuthorities().iterator().next().getAuthority().substring(5);

            LoginResult result = new LoginResult(
                    accessToken,
                    refreshToken,
                    userDetails.getUsername(),
                    role,
                    86400L
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

            if (!jwtUtil.validateToken(refreshToken, userDetails)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(List.of("Refresh token expired or invalid")));
            }

            String newAccessToken = jwtUtil.generateAccessToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);
            String role = userDetails.getAuthorities().iterator().next().getAuthority().substring(5);

            LoginResult result = new LoginResult(
                    newAccessToken,
                    newRefreshToken,
                    userDetails.getUsername(),
                    role,
                    86400L
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(List.of("Failed to refresh token")));
        }
    }
}

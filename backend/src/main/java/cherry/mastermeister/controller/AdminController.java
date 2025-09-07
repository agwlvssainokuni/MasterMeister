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
import cherry.mastermeister.controller.dto.UserSummaryResponse;
import cherry.mastermeister.model.UserSummary;
import cherry.mastermeister.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrative operations")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/pending")
    @Operation(summary = "Get pending user registrations",
            description = "Retrieve all users with PENDING status for admin approval")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved pending users"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - admin role required")
    })
    public ApiResponse<List<UserSummaryResponse>> getPendingUsers() {
        List<UserSummary> pendingUsers = userService.getPendingUsers();
        List<UserSummaryResponse> results = pendingUsers.stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.success(results);
    }

    @PostMapping("/users/{userId}/approve")
    @Operation(summary = "Approve user registration",
            description = "Approve a pending user registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User approved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User is not in pending status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - admin role required")
    })
    public ApiResponse<String> approveUser(
            @Parameter(description = "User ID to approve")
            @PathVariable Long userId) {
        userService.approveUser(userId);
        return ApiResponse.success("User approved successfully");
    }

    @PostMapping("/users/{userId}/reject")
    @Operation(summary = "Reject user registration",
            description = "Reject a pending user registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User rejected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User is not in pending status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - admin role required")
    })
    public ApiResponse<String> rejectUser(
            @Parameter(description = "User ID to reject")
            @PathVariable Long userId) {
        userService.rejectUser(userId);
        return ApiResponse.success("User rejected successfully");
    }

    private UserSummaryResponse toResponse(UserSummary userSummary) {
        return new UserSummaryResponse(
                userSummary.id(),
                userSummary.email(),
                userSummary.status().name(),
                userSummary.createdAt()
        );
    }
}

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
import cherry.mastermeister.model.UserRegistration;
import cherry.mastermeister.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRegistrationService userRegistrationService;

    public UserController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegistrationResult>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserRegistration model = toModel(request);
        UserRegistration registeredUser = userRegistrationService.registerUser(model);
        UserRegistrationResult result = toResult(registeredUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
    }

    @PostMapping("/confirm-email")
    public ApiResponse<EmailConfirmationResult> confirmEmail(
            @Valid @RequestBody EmailConfirmationRequest request) {
        userRegistrationService.confirmEmail(request.token());

        EmailConfirmationResult result = new EmailConfirmationResult(
                "confirmed",
                "Email confirmed successfully. Please wait for administrator approval."
        );
        return ApiResponse.success(result);
    }

    private UserRegistration toModel(UserRegistrationRequest request) {
        return new UserRegistration(
                null,
                request.email(),
                request.password(),
                null,
                request.language(),
                null,
                null
        );
    }

    private UserRegistrationResult toResult(UserRegistration model) {
        return new UserRegistrationResult(
                model.id(),
                model.email(),
                model.email(), // username の代わりに email を使用
                "Registration successful. Please check your email to confirm your account."
        );
    }
}

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
import cherry.mastermeister.model.RegistrationToken;
import cherry.mastermeister.model.UserRegistrationResult;
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

    @PostMapping("/register-email")
    public ResponseEntity<ApiResponse<RegisterEmailResponse>> registerEmail(
            @Valid @RequestBody RegisterEmailRequest request
    ) {
        RegistrationToken result = userRegistrationService.registerEmail(request.email(), request.language());

        // セキュリティ: 既存ユーザーの有無に関係なく同じレスポンス
        RegisterEmailResponse dto = new RegisterEmailResponse(result.email());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterUserResponse>> registerUser(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        UserRegistrationResult registration = userRegistrationService.registerUser(
                request.token(),
                request.email(),
                request.password(),
                request.language()
        );

        RegisterUserResponse result = new RegisterUserResponse(
                registration.id(),
                registration.email()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
    }
}

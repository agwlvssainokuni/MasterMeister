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

import cherry.mastermeister.controller.dto.UserRegistrationRequest;
import cherry.mastermeister.controller.dto.UserRegistrationResult;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.exception.EmailConfirmationException;
import cherry.mastermeister.exception.UserAlreadyExistsException;
import cherry.mastermeister.model.UserRole;
import cherry.mastermeister.model.UserStatus;
import cherry.mastermeister.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@Transactional
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }

    public UserRegistrationResult registerUser(UserRegistrationRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        String emailConfirmationToken = generateEmailConfirmationToken();

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmailConfirmationToken(emailConfirmationToken);
        user.setPreferredLanguage(request.language() != null ? request.language() : "en");
        user.setStatus(UserStatus.PENDING);
        user.setRole(UserRole.USER);

        UserEntity savedUser = userRepository.save(user);

        // メール確認用メール送信
        emailService.sendEmailConfirmation(
                savedUser.getEmail(),
                savedUser.getUsername(),
                emailConfirmationToken,
                request.language()
        );

        return new UserRegistrationResult(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "Registration successful. Please check your email to confirm your account."
        );
    }

    public boolean confirmEmail(String token) {
        return userRepository.findByEmailConfirmationToken(token)
                .map(user -> {
                    if (user.isEmailConfirmed()) {
                        throw new EmailConfirmationException("Email already confirmed");
                    }
                    user.setEmailConfirmed(true);
                    user.setEmailConfirmationToken(null);
                    userRepository.save(user);

                    // メール確認完了通知送信
                    emailService.sendEmailConfirmed(
                            user.getEmail(),
                            user.getUsername(),
                            user.getPreferredLanguage()
                    );

                    return true;
                })
                .orElseThrow(() -> new EmailConfirmationException("Invalid or expired confirmation token"));
    }

    private String generateEmailConfirmationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

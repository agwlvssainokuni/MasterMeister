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

import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.enums.UserRole;
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.exception.EmailConfirmationException;
import cherry.mastermeister.exception.UserAlreadyExistsException;
import cherry.mastermeister.model.UserRegistration;
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

    public UserRegistration registerUser(UserRegistration registration) {

        if (userRepository.findByEmail(registration.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        String emailConfirmationToken = generateEmailConfirmationToken();

        UserEntity user = new UserEntity();
        user.setEmail(registration.email());
        user.setPassword(passwordEncoder.encode(registration.password()));
        user.setEmailConfirmationToken(emailConfirmationToken);
        user.setPreferredLanguage(registration.preferredLanguage());
        user.setStatus(UserStatus.PENDING);
        user.setRole(UserRole.USER);

        UserEntity savedUser = userRepository.save(user);

        // メール確認用メール送信
        emailService.sendEmailConfirmation(
                savedUser.getEmail(),
                emailConfirmationToken,
                savedUser.getPreferredLanguage()
        );

        return toModel(savedUser);
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

    private UserRegistration toModel(UserEntity entity) {
        return new UserRegistration(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getEmailConfirmationToken(),
                entity.getPreferredLanguage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

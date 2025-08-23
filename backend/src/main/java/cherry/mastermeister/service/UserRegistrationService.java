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

import cherry.mastermeister.entity.RegistrationTokenEntity;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.enums.UserRole;
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.exception.UserRegistrationException;
import cherry.mastermeister.model.RegistrationToken;
import cherry.mastermeister.model.UserRegistration;
import cherry.mastermeister.repository.RegistrationTokenRepository;
import cherry.mastermeister.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Transactional
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom;

    public UserRegistrationService(
            UserRepository userRepository,
            RegistrationTokenRepository registrationTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.registrationTokenRepository = registrationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }

    // Step 1: Register email (initiate registration)
    public RegistrationToken registerEmail(String email, String language) {
        // 既存ユーザーチェック
        boolean userExists = userRepository.findByEmail(email).isPresent();

        if (userExists) {
            // 既存ユーザーの場合はtoken=null, expiresAt=nullで返す
            return new RegistrationToken(null, email, null, null, false, LocalDateTime.now());
        }

        // 新規ユーザーの場合の処理
        // 既存のトークンを無効化
        registrationTokenRepository.markTokensAsUsedByEmail(email);

        // 新しいトークンを生成
        String token = generateRegistrationToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        RegistrationTokenEntity tokenEntity = new RegistrationTokenEntity();
        tokenEntity.setEmail(email);
        tokenEntity.setToken(token);
        tokenEntity.setExpiresAt(expiresAt);

        RegistrationTokenEntity savedToken = registrationTokenRepository.save(tokenEntity);

        // 登録開始メール送信
        emailService.sendRegistrationStart(email, token, language);

        return toRegistrationTokenModel(savedToken);
    }

    // Step 3: Register user with token + password
    public UserRegistration registerUser(String token, String email, String password, String language) {
        // Stream APIでトークン検証（セキュリティ: 常に同じエラーメッセージ）
        RegistrationTokenEntity tokenEntity = registrationTokenRepository.findByToken(token)
                .filter(RegistrationTokenEntity::isValid)
                .filter(t -> t.getEmail().equals(email))
                .orElseThrow(() -> new UserRegistrationException("Token and email do not match or token is invalid"));

        // 既存ユーザーチェック
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserRegistrationException("Token and email do not match or token is invalid");
        }

        // トークンを使用済みにマーク
        tokenEntity.setUsed(true);
        registrationTokenRepository.save(tokenEntity);

        // ユーザー登録
        String emailConfirmationToken = generateEmailConfirmationToken();

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailConfirmationToken(emailConfirmationToken);
        user.setPreferredLanguage(language);
        user.setStatus(UserStatus.PENDING);
        user.setRole(UserRole.USER);

        UserEntity savedUser = userRepository.save(user);

        // メール確認送信
        emailService.sendEmailConfirmation(
                savedUser.getEmail(),
                emailConfirmationToken,
                savedUser.getPreferredLanguage()
        );

        return toModel(savedUser);
    }


    private String generateRegistrationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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

    private RegistrationToken toRegistrationTokenModel(RegistrationTokenEntity entity) {
        return new RegistrationToken(
                entity.getId(),
                entity.getEmail(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.isUsed(),
                entity.getCreatedAt()
        );
    }
}

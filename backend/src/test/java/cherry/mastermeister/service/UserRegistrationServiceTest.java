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
import cherry.mastermeister.exception.UserRegistrationException;
import cherry.mastermeister.model.RegistrationToken;
import cherry.mastermeister.model.UserRegistration;
import cherry.mastermeister.repository.RegistrationTokenRepository;
import cherry.mastermeister.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationTokenRepository registrationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(
                userRepository, registrationTokenRepository, passwordEncoder, emailService, 3
        );
    }

    @Test
    void shouldRegisterEmailSuccessfullyForNewUser() {
        String email = "test@example.com";
        String language = "en";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(registrationTokenRepository.markTokensAsUsedByEmail(email)).thenReturn(0);

        RegistrationTokenEntity savedToken = new RegistrationTokenEntity();
        savedToken.setId(1L);
        savedToken.setEmail(email);
        savedToken.setToken("test-token");
        savedToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        savedToken.setCreatedAt(LocalDateTime.now());
        when(registrationTokenRepository.save(any(RegistrationTokenEntity.class))).thenReturn(savedToken);

        RegistrationToken result = userRegistrationService.registerEmail(email, language);

        assertNotNull(result);
        assertEquals(email, result.email());
        assertNotNull(result.token());
        assertNotNull(result.expiresAt());
        assertFalse(result.isExistingUser());

        verify(registrationTokenRepository).save(any(RegistrationTokenEntity.class));
        verify(emailService).sendEmailRegistration(eq(email), anyString(), eq(language));
    }

    @Test
    void shouldReturnExistingUserIndicatorForRegisteredEmail() {
        String email = "test@example.com";
        String language = "en";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new UserEntity()));

        RegistrationToken result = userRegistrationService.registerEmail(email, language);

        assertNotNull(result);
        assertEquals(email, result.email());
        assertNull(result.token());
        assertNull(result.expiresAt());
        assertTrue(result.isExistingUser());

        verify(registrationTokenRepository, never()).save(any(RegistrationTokenEntity.class));
        verify(emailService, never()).sendEmailRegistration(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRegisterUserSuccessfullyWithValidToken() {
        String token = "valid-token";
        String email = "test@example.com";
        String password = "password123";
        String language = "en";

        RegistrationTokenEntity tokenEntity = new RegistrationTokenEntity();
        tokenEntity.setId(1L);
        tokenEntity.setEmail(email);
        tokenEntity.setToken(token);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusHours(1));
        tokenEntity.setUsed(false);

        when(registrationTokenRepository.findByToken(token)).thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(1L);
        savedUser.setEmail(email);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        UserRegistration result = userRegistrationService.registerUser(token, email, password, language);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(email, result.email());

        verify(registrationTokenRepository).save(tokenEntity);
        assertTrue(tokenEntity.isUsed());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionForInvalidToken() {
        String token = "invalid-token";
        String email = "test@example.com";
        String password = "password123";
        String language = "en";

        when(registrationTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(UserRegistrationException.class,
                () -> userRegistrationService.registerUser(token, email, password, language));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionForExpiredToken() {
        String token = "expired-token";
        String email = "test@example.com";
        String password = "password123";
        String language = "en";

        RegistrationTokenEntity tokenEntity = new RegistrationTokenEntity();
        tokenEntity.setEmail(email);
        tokenEntity.setToken(token);
        tokenEntity.setExpiresAt(LocalDateTime.now().minusHours(1));
        tokenEntity.setUsed(false);

        when(registrationTokenRepository.findByToken(token)).thenReturn(Optional.of(tokenEntity));

        assertThrows(UserRegistrationException.class,
                () -> userRegistrationService.registerUser(token, email, password, language));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionForMismatchedEmail() {
        String token = "valid-token";
        String tokenEmail = "token@example.com";
        String requestEmail = "request@example.com";
        String password = "password123";
        String language = "en";

        RegistrationTokenEntity tokenEntity = new RegistrationTokenEntity();
        tokenEntity.setEmail(tokenEmail);
        tokenEntity.setToken(token);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusHours(1));
        tokenEntity.setUsed(false);

        when(registrationTokenRepository.findByToken(token)).thenReturn(Optional.of(tokenEntity));

        assertThrows(UserRegistrationException.class,
                () -> userRegistrationService.registerUser(token, requestEmail, password, language));

        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
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
import cherry.mastermeister.exception.UserAlreadyExistsException;
import cherry.mastermeister.model.UserRegistration;
import cherry.mastermeister.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Test
    void shouldRegisterUserSuccessfully() {
        UserRegistration request = new UserRegistration(
                null,
                "testuser",
                "test@example.com",
                "password123",
                null,
                null,
                null,
                null
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        UserRegistration result = userRegistrationService.registerUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());

        verify(userRepository).save(any(UserEntity.class));
        verify(emailService).sendEmailConfirmation(
                eq("test@example.com"),
                eq("testuser"),
                anyString(),
                eq("en")
        );
    }

    @Test
    void shouldThrowExceptionWhenUsernameExists() {
        UserRegistration request = new UserRegistration(
                null,
                "testuser",
                "test@example.com",
                "password123",
                null,
                null,
                null,
                null
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(UserAlreadyExistsException.class,
                () -> userRegistrationService.registerUser(request));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        UserRegistration request = new UserRegistration(
                null,
                "testuser",
                "test@example.com",
                "password123",
                null,
                null,
                null,
                null
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(UserAlreadyExistsException.class,
                () -> userRegistrationService.registerUser(request));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldConfirmEmailAndSendNotificationWithUserLanguage() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEmailConfirmed(false);
        user.setEmailConfirmationToken("testtoken");
        user.setPreferredLanguage("ja");

        when(userRepository.findByEmailConfirmationToken("testtoken")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        boolean result = userRegistrationService.confirmEmail("testtoken");

        assertTrue(result);
        assertTrue(user.isEmailConfirmed());
        assertNull(user.getEmailConfirmationToken());

        verify(userRepository).save(user);
        verify(emailService).sendEmailConfirmed(
                eq("test@example.com"),
                eq("testuser"),
                eq("ja")
        );
    }
}

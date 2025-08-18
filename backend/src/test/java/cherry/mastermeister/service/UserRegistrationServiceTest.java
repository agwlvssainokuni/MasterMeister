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
import cherry.mastermeister.entity.User;
import cherry.mastermeister.exception.UserAlreadyExistsException;
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

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Test
    void shouldRegisterUserSuccessfully() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "testuser", "test@example.com", "password123", "Test User"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserRegistrationResult result = userRegistrationService.registerUser(request);

        assertNotNull(result);
        assertEquals(1L, result.userId());
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());
        assertNotNull(result.message());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameExists() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "testuser", "test@example.com", "password123", "Test User"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class, 
                () -> userRegistrationService.registerUser(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "testuser", "test@example.com", "password123", "Test User"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class, 
                () -> userRegistrationService.registerUser(request));

        verify(userRepository, never()).save(any(User.class));
    }
}
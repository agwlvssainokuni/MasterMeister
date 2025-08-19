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

import cherry.mastermeister.entity.User;
import cherry.mastermeister.exception.UserNotFoundException;
import cherry.mastermeister.model.UserStatus;
import cherry.mastermeister.model.UserSummary;
import cherry.mastermeister.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldGetPendingUsers() {
        // Arrange
        User user1 = createUser(1L, "user1", UserStatus.PENDING);
        User user2 = createUser(2L, "user2", UserStatus.PENDING);
        when(userRepository.findByStatus(UserStatus.PENDING)).thenReturn(List.of(user1, user2));

        // Act
        List<UserSummary> result = userService.getPendingUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("user1");
        assertThat(result.get(0).status()).isEqualTo(UserStatus.PENDING);
        assertThat(result.get(1).username()).isEqualTo("user2");
        assertThat(result.get(1).status()).isEqualTo(UserStatus.PENDING);

        verify(userRepository).findByStatus(UserStatus.PENDING);
    }

    @Test
    void shouldApproveUser() {
        // Arrange
        User user = createUser(1L, "testuser", UserStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.approveUser(1L);

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.APPROVED);
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(emailService).sendAccountApproved(
                eq("testuser@example.com"),
                eq("testuser"),
                eq("en")
        );
    }

    @Test
    void shouldThrowExceptionWhenApprovingNonExistentUser() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.approveUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void shouldThrowExceptionWhenApprovingNonPendingUser() {
        // Arrange
        User user = createUser(1L, "testuser", UserStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.approveUser(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not in pending status");

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void shouldRejectUser() {
        // Arrange
        User user = createUser(1L, "testuser", UserStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.rejectUser(1L);

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.REJECTED);
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(emailService).sendAccountRejected(
                eq("testuser@example.com"),
                eq("testuser"),
                eq("en")
        );
    }

    @Test
    void shouldThrowExceptionWhenRejectingNonExistentUser() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.rejectUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void shouldThrowExceptionWhenRejectingNonPendingUser() {
        // Arrange
        User user = createUser(1L, "testuser", UserStatus.REJECTED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.rejectUser(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not in pending status");

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(emailService);
    }

    private User createUser(Long id, String username, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName(username.toUpperCase());
        user.setStatus(status);
        user.setEmailConfirmed(true);
        user.setPreferredLanguage("en");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}

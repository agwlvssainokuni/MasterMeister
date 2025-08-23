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
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.exception.UserNotFoundException;
import cherry.mastermeister.model.UserSummary;
import cherry.mastermeister.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public List<UserSummary> getPendingUsers() {
        List<UserEntity> pendingUsers = userRepository.findByStatus(UserStatus.PENDING);
        return pendingUsers.stream()
                .map(this::toUserSummary)
                .toList();
    }

    public void approveUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("User is not in pending status");
        }

        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);

        // 承認通知メール送信
        emailService.sendAccountApproved(
                user.getEmail(),
                user.getPreferredLanguage()
        );
    }

    public void rejectUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("User is not in pending status");
        }

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        // 拒否通知メール送信
        emailService.sendAccountRejected(
                user.getEmail(),
                user.getPreferredLanguage()
        );
    }

    private UserSummary toUserSummary(UserEntity user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}

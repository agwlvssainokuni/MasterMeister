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

package cherry.mastermeister.initializer;

import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.enums.UserRole;
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@Transactional
public class AdminUserInitializer implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean initializeAdminUser;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;
    private final String adminLanguage;

    public AdminUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${mm.admin.initialize}") boolean initializeAdminUser,
            @Value("${mm.admin.username}") String adminUsername,
            @Value("${mm.admin.password}") String adminPassword,
            @Value("${mm.admin.email}") String adminEmail,
            @Value("${mm.admin.language}") String adminLanguage
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initializeAdminUser = initializeAdminUser;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.adminLanguage = adminLanguage;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (initializeAdminUser && !userRepository.existsByEmail(adminEmail)) {
            createAdminUser();
        }
    }

    private void createAdminUser() {
        UserEntity adminUser = new UserEntity();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setStatus(UserStatus.APPROVED);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setPreferredLanguage(adminLanguage);

        userRepository.save(adminUser);

        log.trace("Admin user created: {} / {}", adminUsername, adminPassword);
    }
}

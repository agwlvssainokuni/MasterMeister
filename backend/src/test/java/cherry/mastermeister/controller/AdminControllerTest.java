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

import cherry.mastermeister.config.SecurityConfig;
import cherry.mastermeister.enums.UserStatus;
import cherry.mastermeister.model.UserSummary;
import cherry.mastermeister.service.UserDetailsServiceImpl;
import cherry.mastermeister.service.UserService;
import cherry.mastermeister.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtUtil.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldRequireAdminRoleForPendingUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetPendingUsersWhenAdmin() throws Exception {
        // Arrange
        UserSummary user1 = new UserSummary(
                1L, "user1@example.com",
                UserStatus.PENDING, true, LocalDateTime.now()
        );
        UserSummary user2 = new UserSummary(
                2L, "user2@example.com",
                UserStatus.PENDING, false, LocalDateTime.now()
        );
        when(userService.getPendingUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/pending"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].email").value("user2@example.com"));

        verify(userService).getPendingUsers();
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidNonAdminFromGettingPendingUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users/pending"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRequireAdminRoleForUserApproval() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/approve").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldApproveUserWhenAdmin() throws Exception {
        // Arrange
        doNothing().when(userService).approveUser(eq(1L));

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/1/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").value("User approved successfully"));

        verify(userService).approveUser(eq(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidNonAdminFromApprovingUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/approve").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRequireAdminRoleForUserRejection() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/reject").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectUserWhenAdmin() throws Exception {
        // Arrange
        doNothing().when(userService).rejectUser(eq(1L));

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/1/reject").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").value("User rejected successfully"));

        verify(userService).rejectUser(eq(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidNonAdminFromRejectingUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/reject").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}

package com.osborne.api.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.model.User;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.GoalService;
import com.osborne.api.service.UserService;

import jakarta.persistence.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalUserController.class)
@Import(SecurityConfig.class)
class GoalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID goalId = UUID.randomUUID();

    private User buildUser(String displayName, String email) {
        User user = User.builder()
            .displayName(displayName)
            .email(email)
            .passwordHash("hashed")
            .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Nested
    class GetUsers {

        @Test
        @WithMockUser
        void shouldListGoalUsers() throws Exception {
            var user = buildUser("Bob", "bob@example.com");
            when(goalService.getGoalUsers(goalId)).thenReturn(List.of(user));
            when(userService.toResponse(any(User.class))).thenCallRealMethod();

            mockMvc.perform(get("/api/goals/" + goalId + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Bob"))
                .andExpect(jsonPath("$[0].email").value("bob@example.com"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownGoal() throws Exception {
            when(goalService.getGoalUsers(goalId))
                .thenThrow(new EntityNotFoundException("Goal not found"));

            mockMvc.perform(get("/api/goals/" + goalId + "/users"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            when(goalService.getGoalUsers(goalId))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(get("/api/goals/" + goalId + "/users"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/goals/" + goalId + "/users"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AddUser {

        @Test
        @WithMockUser
        void shouldAddUserToGoal() throws Exception {
            var userId = UUID.randomUUID();

            mockMvc.perform(post("/api/goals/" + goalId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isCreated());

            verify(goalService).addUserToGoal(goalId, userId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownGoal() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Goal not found"))
                .when(goalService).addUserToGoal(eq(goalId), eq(userId));

            mockMvc.perform(post("/api/goals/" + goalId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied"))
                .when(goalService).addUserToGoal(eq(goalId), eq(userId));

            mockMvc.perform(post("/api/goals/" + goalId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/goals/" + goalId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RemoveUser {

        @Test
        @WithMockUser
        void shouldRemoveUserFromGoal() throws Exception {
            var userId = UUID.randomUUID();

            mockMvc.perform(delete("/api/goals/" + goalId + "/users/" + userId))
                .andExpect(status().isNoContent());

            verify(goalService).removeUserFromGoal(goalId, userId);
        }

        @Test
        @WithMockUser
        void shouldReturnConflictWhenLastUser() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new IllegalStateException("Goal must have at least one user"))
                .when(goalService).removeUserFromGoal(eq(goalId), eq(userId));

            mockMvc.perform(delete("/api/goals/" + goalId + "/users/" + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownGoal() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Goal not found"))
                .when(goalService).removeUserFromGoal(eq(goalId), eq(userId));

            mockMvc.perform(delete("/api/goals/" + goalId + "/users/" + userId))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied"))
                .when(goalService).removeUserFromGoal(eq(goalId), eq(userId));

            mockMvc.perform(delete("/api/goals/" + goalId + "/users/" + userId))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/goals/" + goalId + "/users/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }
}

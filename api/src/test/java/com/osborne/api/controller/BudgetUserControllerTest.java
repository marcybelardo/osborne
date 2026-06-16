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
import com.osborne.api.service.BudgetService;
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

@WebMvcTest(BudgetUserController.class)
@Import(SecurityConfig.class)
class BudgetUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID budgetId = UUID.randomUUID();

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
        void shouldListBudgetUsers() throws Exception {
            var user = buildUser("Alice", "alice@example.com");
            when(budgetService.getBudgetUsers(budgetId)).thenReturn(List.of(user));
            when(userService.toResponse(any(User.class))).thenCallRealMethod();

            mockMvc.perform(get("/api/budgets/" + budgetId + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Alice"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownBudget() throws Exception {
            when(budgetService.getBudgetUsers(budgetId))
                .thenThrow(new EntityNotFoundException("Budget not found"));

            mockMvc.perform(get("/api/budgets/" + budgetId + "/users"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            when(budgetService.getBudgetUsers(budgetId))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(get("/api/budgets/" + budgetId + "/users"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/budgets/" + budgetId + "/users"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AddUser {

        @Test
        @WithMockUser
        void shouldAddUserToBudget() throws Exception {
            var userId = UUID.randomUUID();

            mockMvc.perform(post("/api/budgets/" + budgetId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isCreated());

            verify(budgetService).addUserToBudget(budgetId, userId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownBudget() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Budget not found"))
                .when(budgetService).addUserToBudget(eq(budgetId), eq(userId));

            mockMvc.perform(post("/api/budgets/" + budgetId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied"))
                .when(budgetService).addUserToBudget(eq(budgetId), eq(userId));

            mockMvc.perform(post("/api/budgets/" + budgetId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/budgets/" + budgetId + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RemoveUser {

        @Test
        @WithMockUser
        void shouldRemoveUserFromBudget() throws Exception {
            var userId = UUID.randomUUID();

            mockMvc.perform(delete("/api/budgets/" + budgetId + "/users/" + userId))
                .andExpect(status().isNoContent());

            verify(budgetService).removeUserFromBudget(budgetId, userId);
        }

        @Test
        @WithMockUser
        void shouldReturnConflictWhenLastUser() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new IllegalStateException("Budget must have at least one user"))
                .when(budgetService).removeUserFromBudget(eq(budgetId), eq(userId));

            mockMvc.perform(delete("/api/budgets/" + budgetId + "/users/" + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownBudget() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Budget not found"))
                .when(budgetService).removeUserFromBudget(eq(budgetId), eq(userId));

            mockMvc.perform(delete("/api/budgets/" + budgetId + "/users/" + userId))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var userId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied"))
                .when(budgetService).removeUserFromBudget(eq(budgetId), eq(userId));

            mockMvc.perform(delete("/api/budgets/" + budgetId + "/users/" + userId))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/budgets/" + budgetId + "/users/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }
}

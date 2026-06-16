package com.osborne.api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.dto.GoalResponse;
import com.osborne.api.dto.UserSummary;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.GoalService;

import jakarta.persistence.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@Import(SecurityConfig.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private GoalResponse buildGoalResponse() {
        return new GoalResponse(
            UUID.randomUUID(),
            "Test Goal",
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(500),
            LocalDate.now().plusMonths(6),
            50.0,
            List.of(new UserSummary(UUID.randomUUID(), "Test User")),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Nested
    class GetGoals {

        @Test
        @WithMockUser
        void shouldReturnPaginatedGoals() throws Exception {
            var goal = buildGoalResponse();
            var page = new PageImpl<>(List.of(goal), PageRequest.of(0, 20), 1);
            when(goalService.getGoalsForCurrentUser(any())).thenReturn(page);

            mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Goal"))
                .andExpect(jsonPath("$.content[0].targetAmount").value(1000))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetGoal {

        @Test
        @WithMockUser
        void shouldReturnGoalById() throws Exception {
            var goal = buildGoalResponse();
            when(goalService.getGoalById(goal.id())).thenReturn(goal);

            mockMvc.perform(get("/api/goals/" + goal.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Goal"))
                .andExpect(jsonPath("$.targetAmount").value(1000));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(goalService.getGoalById(id))
                .thenThrow(new EntityNotFoundException("Goal not found"));

            mockMvc.perform(get("/api/goals/" + id))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/goals/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class CreateGoal {

        @Test
        @WithMockUser
        void shouldCreateGoal() throws Exception {
            var goal = buildGoalResponse();
            when(goalService.createGoal(any())).thenReturn(goal);

            mockMvc.perform(post("/api/goals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test Goal\",\"targetAmount\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Goal"))
                .andExpect(jsonPath("$.targetAmount").value(1000));
        }

        @Test
        @WithMockUser
        void shouldRejectNullAmount() throws Exception {
            mockMvc.perform(post("/api/goals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void shouldRejectNegativeAmount() throws Exception {
            mockMvc.perform(post("/api/goals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"targetAmount\":-100}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/goals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"targetAmount\":1000}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateGoal {

        @Test
        @WithMockUser
        void shouldUpdateGoal() throws Exception {
            var goal = new GoalResponse(
                UUID.randomUUID(), "Updated Goal", BigDecimal.valueOf(2000),
                BigDecimal.valueOf(500), LocalDate.now().plusMonths(6),
                25.0, List.of(new UserSummary(UUID.randomUUID(), "Test User")),
                List.of(), LocalDateTime.now(), LocalDateTime.now()
            );
            when(goalService.updateGoal(eq(goal.id()), any())).thenReturn(goal);

            mockMvc.perform(put("/api/goals/" + goal.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated Goal\",\"targetAmount\":2000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Goal"))
                .andExpect(jsonPath("$.targetAmount").value(2000));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(goalService.updateGoal(eq(id), any()))
                .thenThrow(new EntityNotFoundException("Goal not found"));

            mockMvc.perform(put("/api/goals/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated\",\"targetAmount\":2000}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var id = UUID.randomUUID();
            when(goalService.updateGoal(eq(id), any()))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(put("/api/goals/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated\",\"targetAmount\":2000}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(put("/api/goals/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated\",\"targetAmount\":2000}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteGoal {

        @Test
        @WithMockUser
        void shouldDeleteGoal() throws Exception {
            var id = UUID.randomUUID();

            mockMvc.perform(delete("/api/goals/" + id))
                .andExpect(status().isNoContent());

            verify(goalService).deleteGoal(id);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new EntityNotFoundException("Goal not found"))
                .when(goalService).deleteGoal(id);

            mockMvc.perform(delete("/api/goals/" + id))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new AccessDeniedException("Access denied"))
                .when(goalService).deleteGoal(id);

            mockMvc.perform(delete("/api/goals/" + id))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/goals/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }
}

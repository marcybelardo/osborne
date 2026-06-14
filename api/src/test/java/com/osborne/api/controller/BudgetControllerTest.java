package com.osborne.api.controller;

import java.math.BigDecimal;
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
import com.osborne.api.dto.BudgetResponse;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.BudgetService;

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

@WebMvcTest(BudgetController.class)
@Import(SecurityConfig.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private BudgetResponse buildBudgetResponse() {
        return new BudgetResponse(
            UUID.randomUUID(),
            BigDecimal.valueOf(500),
            BigDecimal.ZERO,
            List.<UUID>of(UUID.randomUUID()),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Nested
    class GetBudgets {

        @Test
        @WithMockUser
        void shouldReturnPaginatedBudgets() throws Exception {
            var budget = buildBudgetResponse();
            var page = new PageImpl<>(List.of(budget), PageRequest.of(0, 20), 1);
            when(budgetService.getBudgetsForCurrentUser(any())).thenReturn(page);

            mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(500))
                .andExpect(jsonPath("$.content[0].currentSpending").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetBudget {

        @Test
        @WithMockUser
        void shouldReturnBudgetById() throws Exception {
            var budget = buildBudgetResponse();
            when(budgetService.getBudgetById(budget.id())).thenReturn(budget);

            mockMvc.perform(get("/api/budgets/" + budget.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.currentSpending").value(0));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(budgetService.getBudgetById(id))
                .thenThrow(new EntityNotFoundException("Budget not found"));

            mockMvc.perform(get("/api/budgets/" + id))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/budgets/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class CreateBudget {

        @Test
        @WithMockUser
        void shouldCreateBudget() throws Exception {
            var budget = buildBudgetResponse();
            when(budgetService.createBudget(any())).thenReturn(budget);

            mockMvc.perform(post("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.currentSpending").value(0));
        }

        @Test
        @WithMockUser
        void shouldRejectNullAmount() throws Exception {
            mockMvc.perform(post("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void shouldRejectNegativeAmount() throws Exception {
            mockMvc.perform(post("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-100}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":500}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateBudget {

        @Test
        @WithMockUser
        void shouldUpdateBudgetAmount() throws Exception {
            var budget = new BudgetResponse(
                UUID.randomUUID(), BigDecimal.valueOf(1000), BigDecimal.ZERO,
                List.<UUID>of(UUID.randomUUID()), List.of(),
                LocalDateTime.now(), LocalDateTime.now()
            );
            when(budgetService.updateBudget(eq(budget.id()), any())).thenReturn(budget);

            mockMvc.perform(put("/api/budgets/" + budget.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(budgetService.updateBudget(eq(id), any()))
                .thenThrow(new EntityNotFoundException("Budget not found"));

            mockMvc.perform(put("/api/budgets/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":1000}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            var id = UUID.randomUUID();
            when(budgetService.updateBudget(eq(id), any()))
                .thenThrow(new AccessDeniedException("Not the owner"));

            mockMvc.perform(put("/api/budgets/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":1000}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(put("/api/budgets/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":1000}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteBudget {

        @Test
        @WithMockUser
        void shouldDeleteBudget() throws Exception {
            var id = UUID.randomUUID();

            mockMvc.perform(delete("/api/budgets/" + id))
                .andExpect(status().isNoContent());

            verify(budgetService).deleteBudget(id);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new EntityNotFoundException("Budget not found"))
                .when(budgetService).deleteBudget(id);

            mockMvc.perform(delete("/api/budgets/" + id))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            var id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new AccessDeniedException("Not the owner"))
                .when(budgetService).deleteBudget(id);

            mockMvc.perform(delete("/api/budgets/" + id))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/budgets/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }
}

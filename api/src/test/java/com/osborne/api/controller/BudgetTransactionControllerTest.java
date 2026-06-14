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
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.BudgetService;

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

@WebMvcTest(BudgetTransactionController.class)
@Import(SecurityConfig.class)
class BudgetTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID budgetId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();

    private LedgerTransactionResponse buildTransactionResponse() {
        return new LedgerTransactionResponse(
            transactionId,
            BigDecimal.valueOf(-75),
            "Office supplies",
            "Work",
            LocalDate.now(),
            UUID.randomUUID(),
            List.of(budgetId),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Nested
    class GetTransactions {

        @Test
        @WithMockUser
        void shouldListBudgetTransactions() throws Exception {
            var tx = buildTransactionResponse();
            var page = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);
            when(budgetService.getBudgetTransactions(eq(budgetId), any())).thenReturn(page);

            mockMvc.perform(get("/api/budgets/" + budgetId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(-75))
                .andExpect(jsonPath("$.content[0].description").value("Office supplies"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            when(budgetService.getBudgetTransactions(eq(budgetId), any()))
                .thenThrow(new AccessDeniedException("Not the owner"));

            mockMvc.perform(get("/api/budgets/" + budgetId + "/transactions"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/budgets/" + budgetId + "/transactions"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AddTransaction {

        @Test
        @WithMockUser
        void shouldAddTransactionToBudget() throws Exception {
            mockMvc.perform(post("/api/budgets/" + budgetId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + transactionId + "\"}"))
                .andExpect(status().isCreated());

            verify(budgetService).addTransactionToBudget(budgetId, transactionId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownBudget() throws Exception {
            doThrow(new EntityNotFoundException("Budget not found"))
                .when(budgetService).addTransactionToBudget(eq(budgetId), eq(transactionId));

            mockMvc.perform(post("/api/budgets/" + budgetId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + transactionId + "\"}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            doThrow(new AccessDeniedException("Not the owner"))
                .when(budgetService).addTransactionToBudget(eq(budgetId), eq(transactionId));

            mockMvc.perform(post("/api/budgets/" + budgetId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + transactionId + "\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/budgets/" + budgetId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + transactionId + "\"}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RemoveTransaction {

        @Test
        @WithMockUser
        void shouldRemoveTransactionFromBudget() throws Exception {
            mockMvc.perform(delete("/api/budgets/" + budgetId + "/transactions/" + transactionId))
                .andExpect(status().isNoContent());

            verify(budgetService).removeTransactionFromBudget(budgetId, transactionId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownTransaction() throws Exception {
            doThrow(new EntityNotFoundException("Transaction not found in budget"))
                .when(budgetService).removeTransactionFromBudget(eq(budgetId), eq(transactionId));

            mockMvc.perform(delete("/api/budgets/" + budgetId + "/transactions/" + transactionId))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            doThrow(new AccessDeniedException("Not the owner"))
                .when(budgetService).removeTransactionFromBudget(eq(budgetId), eq(transactionId));

            mockMvc.perform(delete("/api/budgets/" + budgetId + "/transactions/" + transactionId))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/budgets/" + budgetId + "/transactions/" + transactionId))
                .andExpect(status().isUnauthorized());
        }
    }
}

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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.GoalService;

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

@WebMvcTest(GoalTransactionController.class)
@Import(SecurityConfig.class)
class GoalTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID goalId = UUID.randomUUID();

    private LedgerTransactionResponse buildTransactionResponse() {
        return new LedgerTransactionResponse(
            UUID.randomUUID(),
            BigDecimal.valueOf(500),
            "Monthly savings transfer",
            "Savings",
            LocalDate.now(),
            UUID.randomUUID(),
            List.of(),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Nested
    class GetTransactions {

        @Test
        @WithMockUser
        void shouldReturnGoalTransactions() throws Exception {
            var tx = buildTransactionResponse();
            when(goalService.getGoalTransactions(goalId)).thenReturn(List.of());

            mockMvc.perform(get("/api/goals/" + goalId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownGoal() throws Exception {
            when(goalService.getGoalTransactions(goalId))
                .thenThrow(new EntityNotFoundException("Goal not found"));

            mockMvc.perform(get("/api/goals/" + goalId + "/transactions"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            when(goalService.getGoalTransactions(goalId))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(get("/api/goals/" + goalId + "/transactions"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/goals/" + goalId + "/transactions"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AddTransaction {

        @Test
        @WithMockUser
        void shouldAddTransactionToGoal() throws Exception {
            var txId = UUID.randomUUID();

            mockMvc.perform(post("/api/goals/" + goalId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + txId + "\"}"))
                .andExpect(status().isCreated());

            verify(goalService).addTransactionToGoal(goalId, txId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownGoal() throws Exception {
            var txId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Goal not found"))
                .when(goalService).addTransactionToGoal(eq(goalId), eq(txId));

            mockMvc.perform(post("/api/goals/" + goalId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + txId + "\"}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var txId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied"))
                .when(goalService).addTransactionToGoal(eq(goalId), eq(txId));

            mockMvc.perform(post("/api/goals/" + goalId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + txId + "\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownTransaction() throws Exception {
            var txId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Transaction not found"))
                .when(goalService).addTransactionToGoal(eq(goalId), eq(txId));

            mockMvc.perform(post("/api/goals/" + goalId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + txId + "\"}"))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/goals/" + goalId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"transactionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RemoveTransaction {

        @Test
        @WithMockUser
        void shouldRemoveTransactionFromGoal() throws Exception {
            var txId = UUID.randomUUID();

            mockMvc.perform(delete("/api/goals/" + goalId + "/transactions/" + txId))
                .andExpect(status().isNoContent());

            verify(goalService).removeTransactionFromGoal(goalId, txId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFoundForUnknownGoal() throws Exception {
            var txId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("Goal not found"))
                .when(goalService).removeTransactionFromGoal(eq(goalId), eq(txId));

            mockMvc.perform(delete("/api/goals/" + goalId + "/transactions/" + txId))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotMember() throws Exception {
            var txId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied"))
                .when(goalService).removeTransactionFromGoal(eq(goalId), eq(txId));

            mockMvc.perform(delete("/api/goals/" + goalId + "/transactions/" + txId))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/goals/" + goalId + "/transactions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }
}

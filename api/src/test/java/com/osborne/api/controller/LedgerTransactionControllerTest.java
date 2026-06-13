package com.osborne.api.controller;

import java.math.BigDecimal;
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
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.LedgerTransactionService;

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

@WebMvcTest(LedgerTransactionController.class)
@Import(SecurityConfig.class)
class LedgerTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerTransactionService ledgerTransactionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID accountId = UUID.randomUUID();

    private LedgerTransaction buildTransaction() {
        return LedgerTransaction.builder()
            .amount(BigDecimal.valueOf(-50))
            .build();
    }

    @Nested
    class GetTransactions {

        @Test
        @WithMockUser
        void shouldReturnPaginatedTransactions() throws Exception {
            var tx = buildTransaction();
            tx.setId(UUID.randomUUID());
            var page = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);
            when(ledgerTransactionService.getTransactionsForAccount(eq(accountId), any()))
                .thenReturn(page);

            mockMvc.perform(get("/api/accounts/" + accountId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(-50))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/accounts/" + accountId + "/transactions"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetTransaction {

        @Test
        @WithMockUser
        void shouldReturnTransactionById() throws Exception {
            var tx = buildTransaction();
            var txId = UUID.randomUUID();
            tx.setId(txId);
            when(ledgerTransactionService.getTransactionById(accountId, txId)).thenReturn(tx);

            mockMvc.perform(get("/api/accounts/" + accountId + "/transactions/" + txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(-50));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var txId = UUID.randomUUID();
            when(ledgerTransactionService.getTransactionById(accountId, txId))
                .thenThrow(new EntityNotFoundException("Transaction not found"));

            mockMvc.perform(get("/api/accounts/" + accountId + "/transactions/" + txId))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/accounts/" + accountId + "/transactions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class CreateTransaction {

        @Test
        @WithMockUser
        void shouldCreateTransaction() throws Exception {
            var tx = buildTransaction();
            tx.setId(UUID.randomUUID());
            when(ledgerTransactionService.createTransaction(eq(accountId), any())).thenReturn(tx);

            mockMvc.perform(post("/api/accounts/" + accountId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(-50));
        }

        @Test
        @WithMockUser
        void shouldRejectNullAmount() throws Exception {
            mockMvc.perform(post("/api/accounts/" + accountId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/accounts/" + accountId + "/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-50}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateTransaction {

        @Test
        @WithMockUser
        void shouldUpdateTransactionAmount() throws Exception {
            var tx = buildTransaction();
            var txId = UUID.randomUUID();
            tx.setAmount(BigDecimal.valueOf(-100));
            tx.setId(txId);
            when(ledgerTransactionService.updateTransaction(eq(accountId), eq(txId), any()))
                .thenReturn(tx);

            mockMvc.perform(put("/api/accounts/" + accountId + "/transactions/" + txId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(-100));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var txId = UUID.randomUUID();
            when(ledgerTransactionService.updateTransaction(eq(accountId), eq(txId), any()))
                .thenThrow(new EntityNotFoundException("Transaction not found"));

            mockMvc.perform(put("/api/accounts/" + accountId + "/transactions/" + txId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-100}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotAccountManager() throws Exception {
            var txId = UUID.randomUUID();
            when(ledgerTransactionService.updateTransaction(eq(accountId), eq(txId), any()))
                .thenThrow(new AccessDeniedException("Not the account manager"));

            mockMvc.perform(put("/api/accounts/" + accountId + "/transactions/" + txId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-100}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(put("/api/accounts/" + accountId + "/transactions/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":-100}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteTransaction {

        @Test
        @WithMockUser
        void shouldDeleteTransaction() throws Exception {
            var txId = UUID.randomUUID();

            mockMvc.perform(delete("/api/accounts/" + accountId + "/transactions/" + txId))
                .andExpect(status().isNoContent());

            verify(ledgerTransactionService).deleteTransaction(accountId, txId);
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var txId = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new EntityNotFoundException("Transaction not found"))
                .when(ledgerTransactionService).deleteTransaction(accountId, txId);

            mockMvc.perform(delete("/api/accounts/" + accountId + "/transactions/" + txId))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotAccountManager() throws Exception {
            var txId = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new AccessDeniedException("Not the account manager"))
                .when(ledgerTransactionService).deleteTransaction(accountId, txId);

            mockMvc.perform(delete("/api/accounts/" + accountId + "/transactions/" + txId))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/accounts/" + accountId + "/transactions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }
}

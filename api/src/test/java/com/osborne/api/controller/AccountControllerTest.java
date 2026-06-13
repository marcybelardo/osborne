package com.osborne.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.dto.CreateAccountRequest;
import com.osborne.api.dto.UpdateAccountRequest;
import com.osborne.api.enums.AccountType;
import com.osborne.api.model.Account;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.AccountService;

import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.persistence.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private Account buildAccount(String name) {
        Account account = Account.builder()
            .name(name)
            .type(AccountType.CASH)
            .currency("USD")
            .initialBalance(BigDecimal.valueOf(1000))
            .build();
        account.setId(UUID.randomUUID());
        return account;
    }

    @Nested
    class GetAccounts {

        @Test
        @WithMockUser
        void shouldReturnPaginatedAccounts() throws Exception {
            var account = buildAccount("Checking");
            var page = new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1);
            when(accountService.getAccountsForCurrentUser(any())).thenReturn(page);

            mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Checking"))
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @WithMockUser
        void shouldReturnEmptyPage() throws Exception {
            var page = new PageImpl<Account>(List.of(), PageRequest.of(0, 20), 0);
            when(accountService.getAccountsForCurrentUser(any())).thenReturn(page);

            mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetAccount {

        @Test
        @WithMockUser
        void shouldReturnAccountById() throws Exception {
            var account = buildAccount("Savings");
            when(accountService.getAccountById(account.getId())).thenReturn(account);

            mockMvc.perform(get("/api/accounts/" + account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Savings"))
                .andExpect(jsonPath("$.type").value("CASH"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(accountService.getAccountById(id))
                .thenThrow(new EntityNotFoundException("Account not found with id: " + id));

            mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotManager() throws Exception {
            var id = UUID.randomUUID();
            when(accountService.getAccountById(id))
                .thenThrow(new AccessDeniedException("User does not manage this account"));

            mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/accounts/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class CreateAccount {

        @Test
        @WithMockUser
        void shouldCreateAccount() throws Exception {
            var account = buildAccount("New Account");
            when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(account);

            mockMvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"New Account","type":"CASH","currency":"USD","initialBalance":1000}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Account"))
                .andExpect(jsonPath("$.type").value("CASH"));
        }

        @Test
        @WithMockUser
        void shouldCreateAccountWithDefaults() throws Exception {
            var account = Account.builder()
                .name("Minimal")
                .type(AccountType.EXPENSE)
                .currency("USD")
                .initialBalance(BigDecimal.ZERO)
                .build();
            account.setId(UUID.randomUUID());
            when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(account);

            mockMvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Minimal","type":"EXPENSE"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.initialBalance").value(0));
        }

        @Test
        @WithMockUser
        void shouldRejectMissingName() throws Exception {
            mockMvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"CASH"}"""))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void shouldRejectMissingType() throws Exception {
            mockMvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Account"}"""))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Account","type":"CASH"}"""))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateAccount {

        @Test
        @WithMockUser
        void shouldUpdateAccountFields() throws Exception {
            var account = buildAccount("Updated");
            account.setType(AccountType.CREDIT_CARD);
            when(accountService.updateAccount(eq(account.getId()), any(UpdateAccountRequest.class)))
                .thenReturn(account);

            mockMvc.perform(put("/api/accounts/" + account.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Updated","type":"CREDIT_CARD"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.type").value("CREDIT_CARD"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(accountService.updateAccount(eq(id), any()))
                .thenThrow(new EntityNotFoundException("Account not found with id: " + id));

            mockMvc.perform(put("/api/accounts/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Does Not Matter"}"""))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotManager() throws Exception {
            var id = UUID.randomUUID();
            when(accountService.updateAccount(eq(id), any()))
                .thenThrow(new AccessDeniedException("User does not manage this account"));

            mockMvc.perform(put("/api/accounts/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Hacked"}"""))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(put("/api/accounts/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }
    }
}

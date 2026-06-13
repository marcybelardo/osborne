package com.osborne.api.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.dto.UserResponse;
import com.osborne.api.model.User;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.UserService;

import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse buildUserResponse() {
        return new UserResponse(
            UUID.randomUUID(),
            "Test User",
            "user@example.com",
            LocalDateTime.now()
        );
    }

    @Nested
    class CreateUser {

        @Test
        @WithMockUser
        void shouldCreateUser() throws Exception {
            var response = buildUserResponse();
            when(userService.createUser(any())).thenReturn(User.builder().build());
            when(userService.toResponse(any(User.class))).thenReturn(response);

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"user@example.com","password":"password123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.id").value(response.id().toString()));
        }

        @Test
        @WithMockUser
        void shouldRejectMissingDisplayName() throws Exception {
            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@example.com","password":"password123"}"""))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void shouldRejectInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"invalid","password":"password123"}"""))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"user@example.com","password":"password123"}"""))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetCurrentUser {

        @Test
        @WithMockUser
        void shouldReturnCurrentUser() throws Exception {
            var response = buildUserResponse();
            when(userService.getCurrentUser()).thenReturn(User.builder().build());
            when(userService.toResponse(any(User.class))).thenReturn(response);

            mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.id").value(response.id().toString()));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
        }
    }
}

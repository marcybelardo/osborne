package com.osborne.api.controller;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.model.User;
import com.osborne.api.repository.UserRepository;
import com.osborne.api.security.JwtUtil;

import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User buildUser(String email) {
        User user = User.builder()
            .displayName("Test User")
            .email(email)
            .passwordHash("hashed-password")
            .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private void mockTokens() {
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
    }

    private void mockAuthDependencies(User user) {
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(
            org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build()
        );
        when(userRepository.save(any(User.class))).thenReturn(user);
    }

    @Nested
    class Register {

        @Test
        void shouldRegisterNewUser() throws Exception {
            var newUser = buildUser("new@example.com");
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            mockAuthDependencies(newUser);
            mockTokens();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"new@example.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.id").value(newUser.getId().toString()));
        }

        @Test
        void shouldRejectDuplicateEmail() throws Exception {
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"taken@example.com","password":"password123"}"""))
                .andExpect(status().isConflict());
        }

        @Test
        void shouldRejectInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"not-an-email","password":"password123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        void shouldRejectShortPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Test User","email":"user@example.com","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        void shouldRejectBlankDisplayName() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"","email":"user@example.com","password":"password123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        }
    }

    @Nested
    class Login {

        @Test
        void shouldLoginWithValidCredentials() throws Exception {
            var user = buildUser("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
            mockAuthDependencies(user);
            mockTokens();

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@example.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.displayName").value("Test User"));
        }

        @Test
        void shouldRejectUnknownEmail() throws Exception {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"unknown@example.com","password":"password123"}"""))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectWrongPassword() throws Exception {
            var user = buildUser("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@example.com","password":"wrongpassword"}"""))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectMissingEmail() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"password":"password123"}"""))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Refresh {

        @Test
        void shouldRefreshToken() throws Exception {
            var user = buildUser("user@example.com");
            user.setRefreshToken("valid-refresh-token");
            var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("user@example.com")
                .password("hashed-password")
                .authorities("ROLE_USER")
                .build();

            when(jwtUtil.extractUsername("valid-refresh-token")).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(jwtUtil.validateToken("valid-refresh-token", userDetails)).thenReturn(true);
            mockTokens();
            when(userRepository.save(any(User.class))).thenReturn(user);

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"refreshToken":"valid-refresh-token"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        void shouldRejectTokenWithNoUsername() throws Exception {
            when(jwtUtil.extractUsername("bad-token")).thenReturn(null);

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"refreshToken":"bad-token"}"""))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectRevokedToken() throws Exception {
            var user = buildUser("user@example.com");
            user.setRefreshToken("different-token");

            when(jwtUtil.extractUsername("stale-token")).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"refreshToken":"stale-token"}"""))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectExpiredToken() throws Exception {
            var user = buildUser("user@example.com");
            user.setRefreshToken("expired-token");
            var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("user@example.com")
                .password("hashed-password")
                .authorities("ROLE_USER")
                .build();

            when(jwtUtil.extractUsername("expired-token")).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(jwtUtil.validateToken("expired-token", userDetails)).thenReturn(false);

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"refreshToken":"expired-token"}"""))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Logout {

        @Test
        @WithMockUser(username = "user@example.com")
        void shouldClearRefreshToken() throws Exception {
            var user = buildUser("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

            verify(userRepository).save(user);
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());

            verify(userRepository, never()).save(any());
        }
    }
}

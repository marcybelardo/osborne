package com.osborne.api.service;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.osborne.api.dto.CreateUserRequest;
import com.osborne.api.dto.UserResponse;
import com.osborne.api.model.User;
import com.osborne.api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(CreateUserRequest request) {
        User user = User.builder()
            .displayName(request.displayName())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();

        return userRepository.save(user);
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        return getUserByEmail(email);
    }

    public User getUserById(UUID id) {
        return userRepository
            .findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("User not found with id: " + id)
            );
    }

    public User getUserByEmail(String email) {
        return userRepository
            .findByEmail(email)
            .orElseThrow(() ->
                new EntityNotFoundException("User not found with email: " + email)
            );
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }

}

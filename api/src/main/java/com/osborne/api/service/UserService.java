package com.osborne.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.osborne.api.dto.CreateUserRequest;
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

    public List<User> getAllUsers() {
	return userRepository.findAll();
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

}

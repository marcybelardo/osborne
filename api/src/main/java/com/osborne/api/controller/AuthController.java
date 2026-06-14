package com.osborne.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import com.osborne.api.dto.AuthResponse;
import com.osborne.api.dto.LoginRequest;
import com.osborne.api.dto.RefreshTokenRequest;
import com.osborne.api.dto.RegisterRequest;
import com.osborne.api.model.User;
import com.osborne.api.repository.UserRepository;
import com.osborne.api.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private AuthResponse buildAuthResponse(User user) {
	var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
	var accessToken = jwtUtil.generateToken(userDetails);
	var refreshToken = jwtUtil.generateRefreshToken(userDetails);

	user.setRefreshTokenHash(jwtUtil.hashToken(refreshToken));
	userRepository.save(user);

	return new AuthResponse(accessToken, refreshToken, user.getDisplayName(), user.getId().toString());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
	@Valid @RequestBody RegisterRequest request
    ) {
	if (userRepository.existsByEmail(request.email())) {
	    throw new ResponseStatusException(
		HttpStatus.CONFLICT,
		"Email is already in use"
	    );
	}

	User user = User.builder()
	    .displayName(request.displayName())
	    .email(request.email())
	    .passwordHash(passwordEncoder.encode(request.password()))
	    .build();

	var savedUser = userRepository.save(user);

	return ResponseEntity.ok(buildAuthResponse(savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
	@Valid @RequestBody LoginRequest request
    ) {
	var user = userRepository
	    .findByEmail(request.email())
	    .orElseThrow(() ->
		new ResponseStatusException(
		    HttpStatus.UNAUTHORIZED,
		    "Invalid credentials"
		)
	    );

	if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
	    throw new ResponseStatusException(
		HttpStatus.UNAUTHORIZED,
		"Invalid credentials"
	    );
	}

	return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
	@Valid @RequestBody RefreshTokenRequest request
    ) {
	var refreshToken = request.refreshToken();
	var userEmail = jwtUtil.extractUsername(refreshToken);

	if (userEmail == null) {
	    throw new ResponseStatusException(
		HttpStatus.UNAUTHORIZED,
		"Invalid refresh token"
	    );
	}

	var user = userRepository
	    .findByEmail(userEmail)
	    .orElseThrow(() ->
		new ResponseStatusException(
		    HttpStatus.UNAUTHORIZED,
		    "User not found"
		)
	    );

	String incomingHash = jwtUtil.hashToken(refreshToken);
	if (!incomingHash.equals(user.getRefreshTokenHash())) {
	    throw new ResponseStatusException(
		HttpStatus.UNAUTHORIZED,
		"Refresh token has been revoked"
	    );
	}

	var userDetails = userDetailsService.loadUserByUsername(userEmail);

	if (!jwtUtil.validateToken(refreshToken, userDetails)) {
	    throw new ResponseStatusException(
		HttpStatus.UNAUTHORIZED,
		"Refresh token expired"
	    );
	}

	return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
	String email = SecurityContextHolder.getContext()
	    .getAuthentication()
	    .getName();

	var user = userRepository
	    .findByEmail(email)
	    .orElseThrow(() ->
		new ResponseStatusException(
		    HttpStatus.UNAUTHORIZED,
		    "User not found"
		)
	    );

	user.setRefreshTokenHash(null);
	userRepository.save(user);

	return ResponseEntity.noContent().build();
    }

}

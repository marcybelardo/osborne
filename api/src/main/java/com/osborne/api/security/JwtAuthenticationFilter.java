package com.osborne.api.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
	HttpServletRequest request,
	HttpServletResponse response,
	FilterChain filterChain
    ) throws ServletException, IOException {
	if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
	    filterChain.doFilter(request, response);
	    return;
	}

	final String authHeader = request.getHeader("Authorization");

	if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	    log.warn(
		"No Authorization header or not Bearer for: {}",
		request.getRequestURI()
	    );
	    filterChain.doFilter(request, response);
	    return;
	}

	try {
	    final String jwt = authHeader.substring(7);
	    log.debug(
		"Extracted JWT (first 20 chars): {}...",
		jwt.substring(0, Math.min(20, jwt.length()))
	    );

	    final String userEmail = jwtUtil.extractUsername(jwt);
	    log.debug("Extracted username from token: {}", userEmail);

	    if (
		userEmail != null &&
		SecurityContextHolder.getContext().getAuthentication() == null
	    ) {
		UserDetails userDetails =
		    this.userDetailsService.loadUserByUsername(userEmail);

		if (jwtUtil.validateToken(jwt, userDetails)) {
		    log.debug(
			"Token validated successfully for user: {}",
			userEmail
		    );

		    UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(
			    userDetails,
			    null,
			    userDetails.getAuthorities()
			);

		    authenticationToken.setDetails(
			new WebAuthenticationDetailsSource().buildDetails(request)
		    );

		    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
		} else {
		    log.warn("Token validation failed for user: {}", userEmail);
		}
	    }
	} catch (Exception e) {
	    log.error("JWT authentication failed: {}", e.getMessage(), e);
	}

	filterChain.doFilter(request, response);
    }

}

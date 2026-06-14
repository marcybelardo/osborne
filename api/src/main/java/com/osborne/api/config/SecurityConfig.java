package com.osborne.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.osborne.api.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final RequestIdFilter requestIdFilter;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
	throws Exception {
	http.csrf(AbstractHttpConfigurer::disable)

	    .cors(cors -> cors.configurationSource(corsConfigurationSource()))

	    .authorizeHttpRequests(auth ->
		auth
		    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh")
		    .permitAll()
		    .requestMatchers("/actuator/health", "/actuator/info")
		    .permitAll()
		    .requestMatchers("/api/auth/**")
		    .authenticated()
		    .anyRequest()
		    .authenticated()
	    )

	    .sessionManagement(session ->
		session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	    )

	    .authenticationProvider(authenticationProvider())

	    .exceptionHandling(exceptions ->
		exceptions.authenticationEntryPoint(
		    (request, response, authException) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType("application/json");
			response
			    .getWriter()
			    .write("{\"error\":\"Invalid credentials\"}");
		    }
		)
	    )

	    .addFilterBefore(
		requestIdFilter,
		UsernamePasswordAuthenticationFilter.class
	    )
	    .addFilterBefore(
		jwtAuthFilter,
		UsernamePasswordAuthenticationFilter.class
	    );

	return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
	DaoAuthenticationProvider authenticationProvider =
	    new DaoAuthenticationProvider(userDetailsService);
	authenticationProvider.setPasswordEncoder(passwordEncoder());

	return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
	AuthenticationConfiguration config
    ) throws Exception {
	return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
	CorsConfiguration configuration = new CorsConfiguration();
	configuration.setAllowedOrigins(allowedOrigins);
	configuration.setAllowedMethods(
	    List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
	);
	configuration.setAllowedHeaders(
	    List.of("Authorization", "Content-Type", "Accept")
	);
	configuration.setAllowCredentials(true);

	UrlBasedCorsConfigurationSource source =
	    new UrlBasedCorsConfigurationSource();
	source.registerCorsConfiguration("/**", configuration);

	return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
	return new BCryptPasswordEncoder();
    }

}

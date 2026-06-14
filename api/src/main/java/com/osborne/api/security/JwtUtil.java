package com.osborne.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateToken(UserDetails userDetails) {
	return buildToken(userDetails, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
	return buildToken(userDetails, refreshExpiration);
    }

    private String buildToken(UserDetails userDetails, long expiration) {
	return Jwts.builder()
	    .subject(userDetails.getUsername())
	    .issuedAt(new Date(System.currentTimeMillis()))
	    .expiration(new Date(System.currentTimeMillis() + expiration))
	    .signWith(getSigningKey())
	    .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
	final String username = extractUsername(token);
	return (
	    (username.equals(userDetails.getUsername())) &&
	    !isTokenExpired(token)
	);
    }

    public String extractUsername(String token) {
	return extractClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpired(String token) {
	return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
	return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(
        String token,
	Function<Claims, T> claimsResolver
    ) {
	final Claims claims = extractAllClaims(token);
	return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
	return Jwts.parser()
	    .verifyWith(getSigningKey())
	    .build()
	    .parseSignedClaims(token)
	    .getPayload();
    }

    @PostConstruct
    void validateSecret() {
	byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
	if (keyBytes.length < 32) {
	    throw new IllegalStateException(
		"JWT secret must be at least 32 bytes (256 bits) for HS256. " +
		"Current length: " + keyBytes.length + " bytes. " +
		"Generate one with: openssl rand -hex 32"
	    );
	}
    }

    public String hashToken(String token) {
	try {
	    MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
	    StringBuilder hexString = new StringBuilder();
	    for (byte b : hash) {
		String hex = Integer.toHexString(0xff & b);
		if (hex.length() == 1) {
		    hexString.append('0');
		}
		hexString.append(hex);
	    }
	    return hexString.toString();
	} catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException("SHA-256 algorithm not available", e);
	}
    }

    private SecretKey getSigningKey() {
	byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
	return Keys.hmacShaKeyFor(keyBytes);
    }

}

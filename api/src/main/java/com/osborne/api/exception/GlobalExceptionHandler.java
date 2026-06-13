package com.osborne.api.exception;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
	Map<String, String> fieldErrors = new HashMap<>();
	for (FieldError error : ex.getBindingResult().getFieldErrors()) {
		fieldErrors.put(error.getField(), error.getDefaultMessage());
	}

	Map<String, Object> response = new HashMap<>();
	response.put("timestamp", LocalDateTime.now());
	response.put("status", HttpStatus.BAD_REQUEST.value());
	response.put("error", "Validation Failed");
	response.put("details", fieldErrors);

	return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
	Map<String, Object> response = new HashMap<>();
	response.put("timestamp", LocalDateTime.now());
	response.put("status", HttpStatus.NOT_FOUND.value());
	response.put("error", "Not Found");
	response.put("details", ex.getMessage());

	return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
	Map<String, Object> response = new HashMap<>();
	response.put("timestamp", LocalDateTime.now());
	response.put("status", HttpStatus.FORBIDDEN.value());
	response.put("error", "Forbidden");
	response.put("details", ex.getMessage());

	return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
	Map<String, Object> response = new HashMap<>();
	response.put("timestamp", LocalDateTime.now());
	response.put("status", HttpStatus.CONFLICT.value());
	response.put("error", "Conflict");
	response.put("details", ex.getMessage());

	return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
	Map<String, Object> response = new HashMap<>();
	response.put("timestamp", LocalDateTime.now());
	response.put("status", ex.getStatusCode().value());
	response.put("error", HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
	response.put("details", ex.getReason());

	return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
	Map<String, Object> response = new HashMap<>();
	response.put("timestamp", LocalDateTime.now());
	response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
	response.put("error", "Internal Server Error");
	response.put("details", "An unexpected error occurred");

	return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

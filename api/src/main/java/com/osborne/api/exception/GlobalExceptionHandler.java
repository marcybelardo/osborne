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

}

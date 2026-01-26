package com.sara.ecom.exception;

import org.hibernate.HibernateException;
import org.hibernate.loader.MultipleBagFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MultipleBagFetchException.class)
    public ResponseEntity<Map<String, String>> handleMultipleBagFetchException(MultipleBagFetchException ex) {
        logger.error("Hibernate MultipleBagFetchException occurred", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Product configuration error. Please check variants and options configuration.");
        error.put("errorCode", "PRODUCT_CONFIG_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(HibernateException.class)
    public ResponseEntity<Map<String, String>> handleHibernateException(HibernateException ex) {
        logger.error("Hibernate exception occurred", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Database operation failed. Please check your product data and try again.");
        error.put("errorCode", "DATABASE_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("errorCode", "NOT_FOUND");
        if (ex.getResourceType() != null) {
            error.put("resourceType", ex.getResourceType());
        }
        if (ex.getIdentifier() != null) {
            error.put("identifier", ex.getIdentifier());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred", ex);
        Map<String, String> error = new HashMap<>();
        // Only expose safe error messages, not internal details
        String message = ex.getMessage();
        if (message != null && (message.contains("Hibernate") || message.contains("MultipleBagFetch"))) {
            error.put("error", "Product configuration error. Please review variants and options.");
            error.put("errorCode", "PRODUCT_CONFIG_ERROR");
        } else {
            error.put("error", message != null ? message : "An error occurred while processing your request.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        logger.error("File upload size exceeded", ex);
        Map<String, Object> error = new HashMap<>();
        error.put("error", "File size exceeds the maximum allowed limit of 10MB");
        error.put("errorCode", "FILE_SIZE_EXCEEDED");
        error.put("source", "validation");
        error.put("maxSize", "10MB");
        error.put("details", "Please upload a file smaller than 10MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unexpected exception occurred", ex);
        Map<String, String> error = new HashMap<>();
        // Never expose internal error details to users
        error.put("error", "An unexpected error occurred. Please try again or contact support.");
        error.put("errorCode", "INTERNAL_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

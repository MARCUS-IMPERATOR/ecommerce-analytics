package com.sqli.ecomAnalytics.exceptions;

import com.sqli.ecomAnalytics.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomerAlreadyExists(
            CustomerAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Customer Already Exists",
                HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomerNotFound(
            CustomerNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Customer Not Found",
                HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({ProductNotFoundException.class, OrderNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(
            RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Resource Not Found",
                HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleProductAlreadyExists(
            ProductAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Product Already Exists",
                HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({InvalidCustomerDataException.class, ProductStockInsufficient.class})
    public ResponseEntity<ErrorResponseDto> handleBadRequest(
            RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Bad Request",
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildErrorResponse(errorMessage, "Validation Failed",
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericError(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        return buildErrorResponse("An unexpected error occurred", "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            String message, String error, HttpStatus status, HttpServletRequest request) {

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message(message)
                .error(error)
                .status(status.value())
                .timestamp(Instant.now().toString())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}
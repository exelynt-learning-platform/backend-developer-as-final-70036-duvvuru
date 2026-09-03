package com.booking.exception;

import com.booking.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(NotFoundException ex, HttpServletRequest request) {
        return ErrorResponse.of(404, "Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noEndpoint(NoResourceFoundException ex, HttpServletRequest request) {
        return ErrorResponse.of(404, "Not Found", "No endpoint " + request.getMethod() + " " + request.getRequestURI(),
                request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflict(ConflictException ex, HttpServletRequest request) {
        return ErrorResponse.of(409, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validationFailed(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ErrorResponse(java.time.LocalDateTime.now(), 400, "Bad Request",
                "Validation failed", request.getRequestURI(), fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse constraintViolated(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("Invalid request parameter");
        return ErrorResponse.of(400, "Bad Request", message, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse unreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ErrorResponse.of(400, "Bad Request",
                "Request body is missing or contains an invalid value (check date format and enum values)",
                request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse missingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return ErrorResponse.of(400, "Bad Request", "Missing query parameter: " + ex.getParameterName(),
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse wrongParamType(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ErrorResponse.of(400, "Bad Request",
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'",
                request.getRequestURI());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse unknownSortField(PropertyReferenceException ex, HttpServletRequest request) {
        return ErrorResponse.of(400, "Bad Request", "Unknown sort property: " + ex.getPropertyName(),
                request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ErrorResponse.of(400, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // @PreAuthorize denials land here before Spring Security's access denied handler
        return ErrorResponse.of(403, "Forbidden", "You don't have permission to access this resource",
                request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse badCredentials(BadCredentialsException ex, HttpServletRequest request) {
        // same message for wrong email and wrong password, so we don't leak which emails exist
        return ErrorResponse.of(401, "Unauthorized", "Invalid email or password", request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse dataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return ErrorResponse.of(409, "Conflict", "The request conflicts with existing data",
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse anythingElse(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ErrorResponse.of(500, "Internal Server Error", "Something went wrong on our side",
                request.getRequestURI());
    }
}

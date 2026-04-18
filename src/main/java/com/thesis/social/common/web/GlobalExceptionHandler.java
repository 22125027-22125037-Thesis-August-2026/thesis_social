package com.thesis.social.common.web;

import com.thesis.social.common.exception.ConflictException;
import com.thesis.social.common.exception.ForbiddenException;
import com.thesis.social.common.exception.NotFoundException;
import com.thesis.social.common.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.social.local/problems/validation"));
        pd.setTitle("Validation failed");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.social.local/problems/constraint-violation"));
        pd.setTitle("Constraint violation");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "resource-not-found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, "resource-conflict", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, "forbidden", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Unexpected error");
    }

    private ProblemDetail build(HttpStatus status, String typeSlug, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create("https://api.social.local/problems/" + typeSlug));
        pd.setTitle(status.getReasonPhrase());
        pd.setDetail(detail);
        return pd;
    }
}

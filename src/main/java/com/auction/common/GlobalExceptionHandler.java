package com.auction.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse> handleBaseException(BaseException exception) {
        return ResponseEntity.badRequest().body(exception.response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseResponse> handleEntitiyNotFound(EntityNotFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BaseResponse(false, "What you find does not exist"));
    }
}
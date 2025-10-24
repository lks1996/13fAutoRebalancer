package com.autoRebalancer.Common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // FilingNotFoundException 전용.
    @ExceptionHandler(FilingNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleFilingNotFound(FilingNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.fail(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // 그 외 모든 에러 처리.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

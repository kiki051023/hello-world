package com.example.student.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDeniedIsReturnedAsForbidden() {
        var result = handler.handleAccessDenied(new AccessDeniedException("scope"));
        assertEquals(403, result.getCode());
    }
}

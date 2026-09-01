package com.example.student.exception;

/** 接口限流触发（M1-2）：HTTP 语义映射 429。 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
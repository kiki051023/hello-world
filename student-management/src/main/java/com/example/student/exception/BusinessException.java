package com.example.student.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}

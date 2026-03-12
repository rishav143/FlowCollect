package com.flowcollect.exception.base;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.code.ErrorCode;

public class BusinessException extends AppException {

    public BusinessException(ErrorCode code,  String message, HttpStatus status) {
        super(code, message, status);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message, status);
    }
}
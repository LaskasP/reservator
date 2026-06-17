package com.skouna.reservator.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(ErrorCodeEnum errorCode, String detail) {
        super(HttpStatus.CONFLICT, errorCode, detail);
    }
}

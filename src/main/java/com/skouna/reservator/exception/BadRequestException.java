package com.skouna.reservator.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(ErrorCodeEnum errorCode, String detail) {
        super(HttpStatus.BAD_REQUEST, errorCode, detail);
    }
}

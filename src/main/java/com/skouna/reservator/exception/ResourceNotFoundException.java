package com.skouna.reservator.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCodeEnum errorCode, String detail) {
        super(HttpStatus.NOT_FOUND, errorCode, detail);
    }
}

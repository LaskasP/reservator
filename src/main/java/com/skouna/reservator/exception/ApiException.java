package com.skouna.reservator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorCodeEnum errorCode;
    private final HttpStatus status;

    protected ApiException(HttpStatus status, ErrorCodeEnum errorCode, String detail) {
        super(detail);
        this.status = status;
        this.errorCode = errorCode;
    }
}

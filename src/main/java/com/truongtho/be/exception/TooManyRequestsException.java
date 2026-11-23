package com.truongtho.be.exception;

import com.truongtho.be.enums.ErrorCode;
import lombok.Getter;

@Getter
public class TooManyRequestsException extends AppException {
    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
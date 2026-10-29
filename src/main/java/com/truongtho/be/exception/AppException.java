package com.truongtho.be.exception;

import com.truongtho.be.enums.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppException extends RuntimeException {
    final ErrorCode code;

    public AppException(ErrorCode code,String message) {
        super(message);
        this.code = code;
    }
    public AppException(String message){
        this(ErrorCode.INVALID_REQUEST, message);
    }

    public AppException(ErrorCode code){
        this(code, code.getMessage());
    }
}

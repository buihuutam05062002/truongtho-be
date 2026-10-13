package com.truongtho.be.exception;

import com.truongtho.be.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode,  String message) {
        super(message);
        this.errorCode = errorCode;
    }

}

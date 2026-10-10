package com.truongtho.be.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum ErrorCode {

    RESOURCE_NOT_FOUND("404", "Resource not found", HttpStatus.NOT_FOUND),
    INVALID_REQUEST("400", "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("401", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("403", "Forbidden", HttpStatus.FORBIDDEN);

    final String code;
    final String message;
    final HttpStatus status;


}

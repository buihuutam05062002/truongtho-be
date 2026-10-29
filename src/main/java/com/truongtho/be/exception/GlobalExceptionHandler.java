package com.truongtho.be.exception;

import com.truongtho.be.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorBody> app(AppException e) {
        return ErrorBody.of(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorBody> business(BusinessException e) {
        return ErrorBody.of(e.getErrorCode());
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorBody> badRequest(BadRequestException e) {
        return ErrorBody.of(ErrorCode.INVALID_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorBody> notFound(ResourceNotFoundException e) {
        return ErrorBody.of(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorBody> noResource(NoResourceFoundException e) {
        return ErrorBody.of(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> details.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ErrorBody.of(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> unreadable(HttpMessageNotReadableException e) {
        return ErrorBody.of(ErrorCode.INVALID_REQUEST, "Nội dung gửi lên không đúng định dạng JSON", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorBody> tooLarge(MaxUploadSizeExceededException e) {
        return ErrorBody.of(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(FileStorageException.class)
    ResponseEntity<ErrorBody> storage(FileStorageException e) {
        log.error("Storage error", e);
        return ErrorBody.of(ErrorCode.INTERNAL_ERROR, e.getMessage(), null);
    }

    // BẮT BUỘC: thiếu 2 handler này thì lỗi @PreAuthorize sẽ bị handler Exception bên dưới biến thành 500
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> denied(AccessDeniedException e) {
        return ErrorBody.of(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorBody> unauthenticated(AuthenticationException e) {
        return ErrorBody.of(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> other(Exception e) {
        log.error("Unhandled exception", e);   // log đầy đủ ở server, không trả stacktrace ra ngoài
        return ErrorBody.of(ErrorCode.INTERNAL_ERROR);
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorBody> state(IllegalStateException e){
        return ErrorBody.of(ErrorCode.INVALID_REQUEST, e.getMessage(), null);
    }
}
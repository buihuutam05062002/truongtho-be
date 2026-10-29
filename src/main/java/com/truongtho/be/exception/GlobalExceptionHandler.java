package com.truongtho.be.exception;

import com.truongtho.be.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
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
import java.util.stream.Collectors;

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
        log.error("Unhandled exception", e);
        return ErrorBody.of(ErrorCode.INTERNAL_ERROR);
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorBody> state(IllegalStateException e){
        return ErrorBody.of(ErrorCode.INVALID_REQUEST, e.getMessage(), null);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(AppException.class)   // AuthService đang dùng, hiện đang ra 500
    public ResponseEntity<Map<String, String>> handleApp(AppException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleLock(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Dữ liệu vừa bị thay đổi, vui lòng thử lại"));
    }
}
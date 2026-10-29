package com.truongtho.be.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.truongtho.be.enums.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorBody {
    Err error;

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static  class Err{
        String code;
        String message;
        Map<String,String> details;
    }

    public static ResponseEntity<ErrorBody> of(ErrorCode c, String msg, Map<String, String> details) {
        return ResponseEntity.status(c.getStatus())
                .body(new ErrorBody(new Err(c.getCode(), msg, details)));
    }

    public static ResponseEntity<ErrorBody> of(ErrorCode c) {
        return of(c, c.getMessage(), null);
    }

}

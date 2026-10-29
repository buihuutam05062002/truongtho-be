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


    INTERNAL_ERROR("1000", "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("1001", "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("1002", "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("1003", "Không tìm thấy dữ liệu", HttpStatus.NOT_FOUND),
    PAYLOAD_TOO_LARGE("1004", "File vượt quá dung lượng cho phép", HttpStatus.PAYLOAD_TOO_LARGE),
    TOO_MANY_REQUESTS("1005", "Bạn thao tác quá nhanh, vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),

    INVALID_CREDENTIALS("2001", "Sai email hoặc mật khẩu", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("2002", "Chưa đăng nhập hoặc phiên đã hết hạn", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("2003", "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("2004", "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("2005", "Tài khoản đã bị khóa hoặc chưa được kích hoạt", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_USED("2006", "Email đã được sử dụng", HttpStatus.CONFLICT),

    RESERVATION_NOT_FOUND("3001", "Không tìm thấy đơn đặt bàn", HttpStatus.NOT_FOUND),
    TABLE_NOT_AVAILABLE("3002", "Bàn đã có người đặt trong khung giờ này", HttpStatus.CONFLICT),
    RESERVATION_TIME_INVALID("3003", "Thời gian đặt bàn không hợp lệ", HttpStatus.BAD_REQUEST);



    final String code;
    final String message;
    final HttpStatus status;


}

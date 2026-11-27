package com.truongtho.be.dto.request;

import com.truongtho.be.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {
    @NotNull
    UserStatus status;
}

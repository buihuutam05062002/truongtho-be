package com.truongtho.be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {
    @NotEmpty
    Set<String> roles;
}

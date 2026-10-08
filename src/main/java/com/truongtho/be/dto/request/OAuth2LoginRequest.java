package com.truongtho.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuth2LoginRequest {
    @NotBlank
    private String credential;
}
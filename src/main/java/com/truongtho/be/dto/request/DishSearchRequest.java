package com.truongtho.be.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DishSearchRequest {
    @NotBlank
    String query;
    @Min(1)
    @Max(10)
    @Builder.Default
    int limit = 5;
}
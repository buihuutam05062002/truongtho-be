package com.truongtho.be.dto.request;

import com.truongtho.be.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProductRequest {
    @NotBlank
    String name;
    @NotNull
            @DecimalMin(value = "0", message = "giá trị không thể nhỏ hơn 0")
    BigDecimal unitPrice;

    String description;
    @NotNull
    ProductStatus productStatus;

    @NotNull
    Long categoryId;

    List<ProductImageRequest> productImageRequests;
}

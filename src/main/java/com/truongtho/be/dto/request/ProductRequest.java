package com.truongtho.be.dto.request;

import com.truongtho.be.enums.ProductStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProductRequest {
    String name;
    BigDecimal unitPrice;
    String description;
    ProductStatus productStatus;

    Long categoryId;

    List<ProductImageRequest> productImageRequests;
}

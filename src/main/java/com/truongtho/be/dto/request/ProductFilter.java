package com.truongtho.be.dto.request;

import com.truongtho.be.enums.ProductSort;
import com.truongtho.be.enums.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductFilter {
    String keyword;
    Long categoryId;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    List<ProductStatus> status;
    ProductSort sortBy = ProductSort.DEFAULT;
}

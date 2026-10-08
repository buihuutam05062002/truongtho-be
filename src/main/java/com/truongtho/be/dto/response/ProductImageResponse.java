package com.truongtho.be.dto.response;

import com.truongtho.be.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProductImageResponse {
    Long id;
    String imageUrl;
    Integer position;
    String altText;

}

package com.truongtho.be.mapper;

import com.truongtho.be.dto.request.ProductImageRequest;
import com.truongtho.be.dto.request.ProductRequest;
import com.truongtho.be.dto.response.ProductImageResponse;
import com.truongtho.be.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {
    ProductImageResponse toResponse(ProductImage image);

}

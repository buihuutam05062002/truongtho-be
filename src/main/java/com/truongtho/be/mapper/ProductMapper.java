package com.truongtho.be.mapper;

import com.truongtho.be.dto.request.ProductRequest;
import com.truongtho.be.dto.response.ProductImageResponse;
import com.truongtho.be.dto.response.ProductResponse;
import com.truongtho.be.entity.Product;
import com.truongtho.be.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = ProductImageMapper.class)
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);

    @Mapping(source = "category", target = "categoryResponse")
    @Mapping(source = "images", target = "productImageResponses")
    ProductResponse toResponse(Product product);
}

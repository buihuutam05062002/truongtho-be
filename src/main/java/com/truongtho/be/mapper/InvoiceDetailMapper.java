package com.truongtho.be.mapper;

import com.truongtho.be.dto.response.InvoiceDetailResponse;
import com.truongtho.be.entity.InvoiceDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceDetailMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    InvoiceDetailResponse toResponse(InvoiceDetail invoiceDetail);
}

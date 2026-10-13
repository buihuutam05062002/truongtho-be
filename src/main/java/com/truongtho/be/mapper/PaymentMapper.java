package com.truongtho.be.mapper;

import com.truongtho.be.dto.response.PaymentResponse;
import com.truongtho.be.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "invoice.id", target = "invoiceId")
    PaymentResponse toResponse(Payment payment);

}
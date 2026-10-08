package com.truongtho.be.mapper;

import com.truongtho.be.dto.request.VoucherRequest;
import com.truongtho.be.dto.response.VoucherResponse;
import com.truongtho.be.entity.Voucher;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    VoucherResponse toResponse(Voucher voucher);
    Voucher toEntity(VoucherRequest voucherRequest);
}

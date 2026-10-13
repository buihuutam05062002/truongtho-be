package com.truongtho.be.mapper;

import com.truongtho.be.dto.response.InvoiceResponse;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.entity.InvoiceDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Set;

@Mapper(componentModel = "spring",  uses = InvoiceDetailMapper.class)
public interface InvoiceMapper {

    @Mapping(source = "diningTable.name", target = "tableName")
    @Mapping(source = "employee.users.username", target = "employeeName")
    @Mapping(source = "voucher.code", target = "voucherCode")
    @Mapping(source = "invoiceDetails", target = "details")
    @Mapping(source = "invoiceDetails", target = "subTotal", qualifiedByName = "sumDetails")
    InvoiceResponse toResponse(Invoice invoice);

    @Named("sumDetails")
    default BigDecimal sumDetails(Set<InvoiceDetail> details) {
        return details.stream()
                .map(InvoiceDetail::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
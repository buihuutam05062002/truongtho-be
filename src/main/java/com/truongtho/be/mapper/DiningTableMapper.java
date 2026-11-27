package com.truongtho.be.mapper;

import com.truongtho.be.dto.request.DiningTableRequest;
import com.truongtho.be.dto.response.DiningTableResponse;
import com.truongtho.be.entity.DiningTable;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DiningTableMapper {
    DiningTableResponse toResponse(DiningTable table);

    DiningTable toEntity(DiningTableRequest diningTableRequest);

    void updateEntity(@MappingTarget DiningTable table, DiningTableRequest diningTableRequest);
}

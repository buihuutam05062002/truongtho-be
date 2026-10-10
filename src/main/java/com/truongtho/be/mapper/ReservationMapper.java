package com.truongtho.be.mapper;

import com.truongtho.be.dto.response.ReservationResponse;
import com.truongtho.be.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {


    ReservationResponse toResponse(Reservation reservation);
}

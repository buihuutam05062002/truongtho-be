package com.truongtho.be.service;

import com.truongtho.be.dto.request.ReservationRequest;
import com.truongtho.be.dto.response.ReservationResponse;
import com.truongtho.be.entity.Reservation;
import com.truongtho.be.enums.ReservationStatus;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.ReservationMapper;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.DiningTableRepository;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.ReservationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationService {
    final ReservationRepository reservationRepository;
    final CustomerRepository customerRepository;
    final EmployeeRepository employeeRepository;
    final DiningTableRepository diningTableRepository;
    final ReservationMapper reservationMapper;

    @Transactional
    public ReservationResponse create(ReservationRequest reservationRequest){
        Reservation reservation = new Reservation();

        reservation.setReservationCode("RES" + UUID.randomUUID().toString().substring(2,8).toUpperCase());

        reservation.setContactName(reservationRequest.getContactName());
        reservation.setContactPhone(reservationRequest.getContactPhone());
        reservation.setReservationTime(reservationRequest.getReservationTime());
        reservation.setPartySize(reservationRequest.getPartySize());
        reservation.setNote(reservationRequest.getNote());
        reservation.setReservationStatus(ReservationStatus.PENDING);


        Reservation saveReservation = reservationRepository.save(reservation);

        return reservationMapper.toResponse(saveReservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id){
        Reservation reservation  = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));

        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest reservationRequest){
        Reservation reservation  = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));

        if((reservation.getReservationStatus() != ReservationStatus.PENDING) && (reservation.getReservationStatus() != ReservationStatus.CONFIRM )){
            throw new IllegalStateException("Chỉ có đặt bàn đang chờ hoặc đã chấp nhận mới được phép sửa");
        }

        reservation.setContactPhone(reservationRequest.getContactPhone());
        reservation.setContactName(reservationRequest.getContactName());
        reservation.setNote(reservationRequest.getNote());
        reservation.setPartySize(reservationRequest.getPartySize());
        reservation.setReservationTime(reservationRequest.getReservationTime());

        Reservation save = reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public void deleteReservation(Long id){
        Reservation reservation  = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));
        if((reservation.getReservationStatus() != ReservationStatus.PENDING) ){
            throw new IllegalStateException("Chỉ có đặt bàn đang chờ mới đươc xóa");
        }
        reservationRepository.delete(reservation);
    }


    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAllReservations(Pageable pageable){
        return reservationRepository.findAll(pageable)
                .map(reservationMapper::toResponse);
    }

}

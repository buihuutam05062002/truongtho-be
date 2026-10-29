package com.truongtho.be.controller;

import com.truongtho.be.dto.request.ReservationRequest;
import com.truongtho.be.dto.response.ReservationResponse;
import com.truongtho.be.service.ReservationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationController {
    final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest reservationRequest){
        ReservationResponse reservationResponse = reservationService.create(reservationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationResponse);
    }
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getAllReservation(Pageable pageable){
        return ResponseEntity.ok(reservationService.getAllReservations(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationId(@PathVariable Long id){
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(@PathVariable Long id, @Valid @RequestBody ReservationRequest reservationRequest){
        return ResponseEntity.ok(reservationService.updateReservation(id,reservationRequest));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id){
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}

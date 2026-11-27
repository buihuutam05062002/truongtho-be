package com.truongtho.be.controller;

import com.truongtho.be.dto.request.ReservationRequest;
import com.truongtho.be.dto.request.ReservationStatusRequest;
import com.truongtho.be.dto.response.ReservationResponse;
import com.truongtho.be.exception.TooManyRequestsException;
import com.truongtho.be.security.FixedWindowLimiter;
import com.truongtho.be.service.ReservationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationController {
    final ReservationService reservationService;
    final FixedWindowLimiter reservationLimiter = new FixedWindowLimiter(5, Duration.ofMinutes(1));
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest reservationRequest, Authentication auth) {
        boolean byStaff = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_STAFF") || a.equals("ROLE_ADMIN"));
        if (!byStaff && !reservationLimiter.tryAcquire(auth.getName())) {
            throw new TooManyRequestsException(
                    "Bạn đặt bàn quá nhanh, vui lòng thử lại sau 1 phút", reservationLimiter.windowSeconds());
        }


        ReservationResponse reservationResponse =
                reservationService.create(reservationRequest, auth.getName(), byStaff);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationResponse);
    }

    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getAllReservation(Pageable pageable){
        return ResponseEntity.ok(reservationService.getAllReservations(pageable));
    }


    @GetMapping("/me")
    public ResponseEntity<Page<ReservationResponse>> myReservations(
            Authentication auth,
            @PageableDefault(size = 10, sort = "reservationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reservationService.getMyReservations(auth.getName(), pageable));
    }

    @PutMapping("/me/{id}")
    public ResponseEntity<ReservationResponse> updateMyReservation(
            @PathVariable Long id, @Valid @RequestBody ReservationRequest request, Authentication auth) {
        return ResponseEntity.ok(reservationService.updateMine(id, request, auth.getName()));
    }

    @PatchMapping("/me/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelMyReservation(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservationService.cancelMine(id, auth.getName()));
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody ReservationStatusRequest request) {
      return ResponseEntity.ok(reservationService.updateStatus(id,request.getReservationStatus(),request.getTableId()));

    }
}

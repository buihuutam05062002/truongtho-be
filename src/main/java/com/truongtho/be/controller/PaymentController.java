package com.truongtho.be.controller;

import com.truongtho.be.dto.request.PaymentRequest;
import com.truongtho.be.dto.response.PaymentResponse;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class PaymentController {

    final PaymentService paymentService;

    @PostMapping("/invoices/{invoiceId}")
    public ResponseEntity<PaymentResponse> create(@PathVariable String invoiceId,
                                                  @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(invoiceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable String id, Authentication auth) {
        if (auth == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Không đủ quyền hạn");
        }
        return ResponseEntity.ok(paymentService.confirmManualPayment(id, auth.getName()));
    }
}
package com.truongtho.be.controller;

import com.truongtho.be.dto.request.InvoiceDetailRequest;
import com.truongtho.be.dto.request.SePayWebhookRequest;
import com.truongtho.be.dto.response.InvoiceResponse;
import com.truongtho.be.dto.response.PaymentResponse;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.service.InvoiceService;
import com.truongtho.be.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
//@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class InvoiceController {
    final InvoiceService invoiceService;
    final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@RequestParam Long tableId, Authentication auth) {
        InvoiceResponse response = invoiceService.createInvoice(tableId, currentUserId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping("/{id}/details")
    public ResponseEntity<InvoiceResponse> addDetail(@PathVariable String id,
                                                     @Valid @RequestBody InvoiceDetailRequest request) {
        return ResponseEntity.ok(invoiceService.addDetail(id, request));
    }

    @PutMapping("/{id}/details/{productId}")
    public ResponseEntity<InvoiceResponse> updateDetail(@PathVariable String id,
                                                        @PathVariable Long productId,
                                                        @RequestParam Integer quantity) {
        return ResponseEntity.ok(invoiceService.updateDetail(id, productId, quantity));
    }

    @DeleteMapping("/{id}/details/{productId}")
    public ResponseEntity<InvoiceResponse> removeDetail(@PathVariable String id,
                                                        @PathVariable Long productId) {
        return ResponseEntity.ok(invoiceService.removeDetail(id, productId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        invoiceService.cancelInvoice(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> search(
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.search(tableId, status, pageable));
    }

    @GetMapping("/tables/{tableId}/current")
    public ResponseEntity<InvoiceResponse> getOpenInvoiceByTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(invoiceService.getOpenInvoiceByTable(tableId));
    }
    @PostMapping("/{id}/payments/sepay")
    public ResponseEntity<PaymentResponse> createOnlinePayment(@PathVariable("id") String id){
        return ResponseEntity.ok(paymentService.createSePayPayment(id));
    }





    private String currentUserId(Authentication auth) {
        if (auth == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Không có thẩm quyền");
        }
        return auth.getName(); }
}

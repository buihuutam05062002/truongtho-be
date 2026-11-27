package com.truongtho.be.controller;

import com.truongtho.be.dto.request.ApplyVoucherRequest;
import com.truongtho.be.dto.request.AttachCustomerRequest;
import com.truongtho.be.dto.request.InvoiceDetailRequest;
import com.truongtho.be.dto.request.RedeemPointsRequest;
import com.truongtho.be.dto.request.SePayWebhookRequest;
import com.truongtho.be.dto.response.InvoiceResponse;
import com.truongtho.be.dto.response.PaymentResponse;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.service.InvoiceService;
import com.truongtho.be.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class InvoiceController {
    final InvoiceService invoiceService;
    final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@RequestParam Long tableId, Authentication auth) {
        InvoiceResponse response = invoiceService.createInvoice(tableId, currentUserId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<InvoiceResponse>> myOpenInvoices(Authentication auth) {
        return ResponseEntity.ok(invoiceService.findMyOpenInvoices(currentUserId(auth)));
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

    @PutMapping("/{id}/customer")
    public ResponseEntity<InvoiceResponse> attachCustomer(@PathVariable String id,
                                                          @Valid @RequestBody AttachCustomerRequest request) {
        return ResponseEntity.ok(invoiceService.attachCustomer(id, request.getKeyword()));
    }

    @DeleteMapping("/{id}/customer")
    public ResponseEntity<InvoiceResponse> detachCustomer(@PathVariable String id) {
        return ResponseEntity.ok(invoiceService.detachCustomer(id));
    }

    @PutMapping("/{id}/voucher")
    public ResponseEntity<InvoiceResponse> applyVoucher(@PathVariable String id,
                                                        @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(invoiceService.applyVoucher(id, request.getCode()));
    }

    @DeleteMapping("/{id}/voucher")
    public ResponseEntity<InvoiceResponse> removeVoucher(@PathVariable String id) {
        return ResponseEntity.ok(invoiceService.removeVoucher(id));
    }

    @PutMapping("/{id}/points")
    public ResponseEntity<InvoiceResponse> redeemPoints(@PathVariable String id,
                                                        @Valid @RequestBody RedeemPointsRequest request) {
        return ResponseEntity.ok(invoiceService.redeemPoints(id, request.getPoints()));
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

    @PostMapping("/{id}/move-table")
    public ResponseEntity<InvoiceResponse> moveTable(@PathVariable String id,
                                                     @RequestParam Long toTableId) {
        return ResponseEntity.ok(invoiceService.moveTable(id, toTableId));
    }

    @PostMapping("/{id}/merge-table")
    public ResponseEntity<InvoiceResponse> mergeTable(@PathVariable String id,
                                                      @RequestParam Long fromTableId) {
        return ResponseEntity.ok(invoiceService.mergeTable(id, fromTableId));
    }





    private String currentUserId(Authentication auth) {
        if (auth == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Không có thẩm quyền");
        }
        return auth.getName(); }
}

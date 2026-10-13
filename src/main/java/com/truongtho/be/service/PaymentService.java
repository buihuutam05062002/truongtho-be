package com.truongtho.be.service;

import com.truongtho.be.dto.request.PaymentRequest;
import com.truongtho.be.dto.response.PaymentResponse;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.entity.Payment;
import com.truongtho.be.enums.*;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.PaymentMapper;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.InvoiceRepository;
import com.truongtho.be.repository.PaymentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.truongtho.be.dto.request.SePayWebhookRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {

    PaymentRepository paymentRepository;
    InvoiceRepository invoiceRepository;
    EmployeeRepository employeeRepository;
    PaymentMapper paymentMapper;
    SepayService sepayService;

    @Transactional
    public PaymentResponse createPayment(String invoiceId, PaymentRequest request) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));

        if (invoice.getInvoiceStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Hóa đơn không ở trạng thái chờ thanh toán");
        }
        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Hóa đơn phải có tổng tiền lớn hơn 0");
        }
        Payment payment = paymentRepository
                .findByInvoice_IdAndPaymentStatus(invoiceId, PaymentStatus.PENDING)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setInvoice(invoice);
                    p.setPaymentStatus(PaymentStatus.PENDING);
                    p.setCreatedAt(LocalDateTime.now());
                    p.setTransactionCode(generatorTransactionCode());
                    return p;
                });
        payment.setAmount(invoice.getTotalAmount());
        payment.setPaymentMethod(request.getPaymentMethod());

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirmManualPayment(String paymentId, String employeeId) {
        String invoiceId = paymentRepository.findInvoiceIdById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Thanh toán không còn ở trạng thái chờ");
        }
        if (invoice.getInvoiceStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Hóa đơn không còn ở trạng thái chờ thanh toán");
        }
        if (payment.getPaymentMethod() == PaymentMethod.ONLINE) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Thanh toán online không xác nhận thủ công");
        }
        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Hóa đơn phải có tổng tiền lớn hơn 0");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + employeeId));

        payment.setAmount(invoice.getTotalAmount());
        payment.setEmployee(employee);


        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());

        invoice.setInvoiceStatus(InvoiceStatus.PAID);

        DiningTable table = invoice.getDiningTable();
        if (table != null && table.getTableStatus() == TableStatus.SERVING) {
            table.setTableStatus(TableStatus.AVAILABLE);
        }

        return paymentMapper.toResponse(payment);
    }







    private String generatorTransactionCode(){
        return  "TT" +  UUID.randomUUID()
                .toString()
                .replace("-","")
                .substring(1,20)
                .toUpperCase();
    }


    @Transactional
    public PaymentResponse createSePayPayment(String invoiceId){
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));
        if (invoice.getInvoiceStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Hóa đơn không ở trạng thái chờ thanh toán");
        }
        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Hóa đơn phải có tổng tiền lớn hơn 0");
        }
        Payment payment = paymentRepository
                .findByInvoice_IdAndPaymentStatus(invoiceId, PaymentStatus.PENDING)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setInvoice(invoice);
                    p.setPaymentMethod(PaymentMethod.ONLINE);
                    p.setPaymentStatus(PaymentStatus.PENDING);
                    p.setCreatedAt(LocalDateTime.now());
                    p.setTransactionCode(generatorTransactionCode());
                    return p;
                });

        payment.setAmount(invoice.getTotalAmount());
        payment.setPaymentMethod(PaymentMethod.ONLINE);
        paymentRepository.save(payment);
        String url = sepayService.buildQrUrl(payment);

        return PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(invoiceId)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .QrUrl(url)
                .build();
    }



    @Transactional
    public void handleSePayWebhook(SePayWebhookRequest req){
        if (!"in".equalsIgnoreCase(req.getTransferType())) {
            return;
        }
        String code = sepayService.extractPaymentCode(req);
        if (code == null) {
            log.warn("SePay webhook {} không có mã thanh toán, content={}", req.getId(), req.getContent());
            return;
        }

        if (paymentRepository.existsBySepayTransactionId(req.getId())) {
            log.info("SePay webhook {} đã xử lý trước đó", req.getId());
            return;
        }

        String invoiceId = paymentRepository.findInvoiceIdByTransactionCode(code).orElse(null);
        if (invoiceId == null) {
            log.warn("Không tìm thấy payment cho mã {}", code);
            return;
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));
        Payment payment = paymentRepository.findByTransactionCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + code));


        if (payment.getPaymentStatus() != PaymentStatus.PENDING
                || invoice.getInvoiceStatus() != InvoiceStatus.PENDING
                || payment.getPaymentMethod() != PaymentMethod.ONLINE) {
            log.warn("Bỏ qua webhook {}: payment={}, invoice={}, method={}",
                    req.getId(), payment.getPaymentStatus(),
                    invoice.getInvoiceStatus(), payment.getPaymentMethod());
            return;
        }
        BigDecimal paid = BigDecimal.valueOf(req.getTransferAmount());
        if (paid.compareTo(payment.getAmount()) < 0) {
            log.warn("Chuyển thiếu tiền: nhận {} < cần {} (mã {})", paid, payment.getAmount(), code);
            return;
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setSepayTransactionId(req.getId());

        invoice.setInvoiceStatus(InvoiceStatus.PAID);

        DiningTable table = invoice.getDiningTable();
        if (table != null && table.getTableStatus() == TableStatus.SERVING) {
            table.setTableStatus(TableStatus.AVAILABLE);
        }
    }



}
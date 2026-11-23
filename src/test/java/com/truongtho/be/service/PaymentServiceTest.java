package com.truongtho.be.service;

import com.truongtho.be.dto.request.PaymentRequest;
import com.truongtho.be.dto.request.SePayWebhookRequest;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.entity.Payment;
import com.truongtho.be.entity.Reservation;
import com.truongtho.be.entity.Voucher;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.enums.PaymentMethod;
import com.truongtho.be.enums.PaymentStatus;
import com.truongtho.be.enums.ReservationStatus;
import com.truongtho.be.enums.TableStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.PaymentMapper;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.InvoiceRepository;
import com.truongtho.be.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock PaymentMapper paymentMapper;
    @Mock SepayService sepayService;
    @Mock LoyaltyService loyaltyService;
    @Mock VoucherService voucherService;
    @InjectMocks PaymentService service;

    // ---------- helpers ----------

    private Invoice invoice(String id, InvoiceStatus status, long total) {
        Invoice i = new Invoice();
        i.setId(id);
        i.setInvoiceStatus(status);
        i.setTotalAmount(BigDecimal.valueOf(total));
        return i;
    }

    private Payment payment(String id, Invoice inv, PaymentMethod method, PaymentStatus status) {
        Payment p = new Payment();
        p.setId(id);
        p.setInvoice(inv);
        p.setPaymentMethod(method);
        p.setPaymentStatus(status);
        p.setAmount(inv.getTotalAmount());
        p.setTransactionCode("TT123");
        return p;
    }

    private DiningTable table(TableStatus status) {
        DiningTable t = new DiningTable();
        t.setId(1L);
        t.setTableStatus(status);
        return t;
    }

    private Reservation reservation(ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(1L);
        r.setReservationStatus(status);
        return r;
    }

    private SePayWebhookRequest webhook(long id, String type, long amount) {
        SePayWebhookRequest r = new SePayWebhookRequest();
        r.setId(id);
        r.setTransferType(type);
        r.setTransferAmount(amount);
        r.setContent("TT123");
        return r;
    }

    private void stubManual(Payment p, Invoice inv) {
        when(paymentRepository.findInvoiceIdById(p.getId())).thenReturn(Optional.of(inv.getId()));
        when(invoiceRepository.findByIdForUpdate(inv.getId())).thenReturn(Optional.of(inv));
        when(paymentRepository.findById(p.getId())).thenReturn(Optional.of(p));
    }

    private void stubWebhook(Invoice inv, Payment p) {
        when(sepayService.extractPaymentCode(any())).thenReturn("TT123");
        when(paymentRepository.existsBySepayTransactionId(anyLong())).thenReturn(false);
        when(paymentRepository.findInvoiceIdByTransactionCode("TT123")).thenReturn(Optional.of(inv.getId()));
        when(invoiceRepository.findByIdForUpdate(inv.getId())).thenReturn(Optional.of(inv));
        when(paymentRepository.findByTransactionCode("TT123")).thenReturn(Optional.of(p));
    }


    @Test
    void createPayment_invoiceNotFound_throws() {
        when(invoiceRepository.findByIdForUpdate("inv1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayment("inv1", new PaymentRequest(PaymentMethod.CASH)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPayment_invoiceAlreadyPaid_throws() {
        when(invoiceRepository.findByIdForUpdate("inv1"))
                .thenReturn(Optional.of(invoice("inv1", InvoiceStatus.PAID, 100_000)));

        assertThatThrownBy(() -> service.createPayment("inv1", new PaymentRequest(PaymentMethod.CASH)))
                .isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_zeroTotal_throws() {
        when(invoiceRepository.findByIdForUpdate("inv1"))
                .thenReturn(Optional.of(invoice("inv1", InvoiceStatus.PENDING, 0)));

        assertThatThrownBy(() -> service.createPayment("inv1", new PaymentRequest(PaymentMethod.CASH)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lớn hơn 0");
    }

    @Test
    void createPayment_newPayment_copiesInvoiceTotal() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 250_000);
        when(invoiceRepository.findByIdForUpdate("inv1")).thenReturn(Optional.of(inv));
        when(paymentRepository.findByInvoice_IdAndPaymentStatus("inv1", PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createPayment("inv1", new PaymentRequest(PaymentMethod.CASH));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("250000");
        assertThat(saved.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(saved.getValue().getTransactionCode()).startsWith("TT");
    }

    @Test
    void createPayment_reusesExistingPendingPayment() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 300_000);
        Payment existing = payment("pay1", inv, PaymentMethod.CASH, PaymentStatus.PENDING);
        existing.setAmount(BigDecimal.valueOf(100_000)); // tổng tiền hóa đơn đã đổi sau đó
        when(invoiceRepository.findByIdForUpdate("inv1")).thenReturn(Optional.of(inv));
        when(paymentRepository.findByInvoice_IdAndPaymentStatus("inv1", PaymentStatus.PENDING))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createPayment("inv1", new PaymentRequest(PaymentMethod.BANK_TRANSFER));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(existing);
        assertThat(saved.getValue().getTransactionCode()).isEqualTo("TT123");
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("300000");
        assertThat(saved.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
    }



    @Test
    void confirmManual_paymentNotFound_throws() {
        when(paymentRepository.findInvoiceIdById("pay1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmManualPayment("pay1", "emp1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmManual_onlinePayment_rejected() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        stubManual(p, inv);

        assertThatThrownBy(() -> service.confirmManualPayment("pay1", "emp1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("online");
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void confirmManual_paymentNotPending_rejected() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.CASH, PaymentStatus.SUCCESS);
        stubManual(p, inv);

        assertThatThrownBy(() -> service.confirmManualPayment("pay1", "emp1"))
                .isInstanceOf(BusinessException.class);
        verify(loyaltyService, never()).awardPoints(any());
    }

    @Test
    void confirmManual_employeeNotFound_changesNothing() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.CASH, PaymentStatus.PENDING);
        stubManual(p, inv);
        when(employeeRepository.findById("emp1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmManualPayment("pay1", "emp1"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirmManual_success_closesEverything() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 200_000);
        DiningTable table = table(TableStatus.SERVING);
        Reservation res = reservation(ReservationStatus.ARRIVED);
        Voucher voucher = mock(Voucher.class);
        inv.setDiningTable(table);
        inv.setReservation(res);
        inv.setVoucher(voucher);
        Payment p = payment("pay1", inv, PaymentMethod.CASH, PaymentStatus.PENDING);
        Employee emp = mock(Employee.class);
        stubManual(p, inv);
        when(employeeRepository.findById("emp1")).thenReturn(Optional.of(emp));

        service.confirmManualPayment("pay1", "emp1");

        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(p.getPaidAt()).isNotNull();
        assertThat(p.getEmployee()).isSameAs(emp);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(table.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(res.getReservationStatus()).isEqualTo(ReservationStatus.COMPLETED);
        verify(loyaltyService).awardPoints(inv);
        verify(voucherService).consume(voucher);
    }


    @Test
    void webhook_moneyOut_ignored() {
        service.handleSePayWebhook(webhook(1, "out", 100_000));

        verifyNoInteractions(sepayService, paymentRepository, invoiceRepository);
    }

    @Test
    void webhook_noPaymentCode_ignored() {
        when(sepayService.extractPaymentCode(any())).thenReturn(null);

        service.handleSePayWebhook(webhook(1, "in", 100_000));

        verifyNoInteractions(invoiceRepository);
        verify(paymentRepository, never()).findByTransactionCode(any());
    }

    @Test
    void webhook_duplicateTransaction_processedOnce() {
        when(sepayService.extractPaymentCode(any())).thenReturn("TT123");
        when(paymentRepository.existsBySepayTransactionId(99L)).thenReturn(true);

        service.handleSePayWebhook(webhook(99, "in", 100_000));

        verifyNoInteractions(invoiceRepository, loyaltyService);
    }

    @Test
    void webhook_underpaid_staysPending() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        stubWebhook(inv, p);

        service.handleSePayWebhook(webhook(1, "in", 99_999));

        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PENDING);
        verify(loyaltyService, never()).awardPoints(any());
    }

    @Test
    void webhook_exactAmount_marksPaid() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        DiningTable table = table(TableStatus.SERVING);
        Reservation res = reservation(ReservationStatus.ARRIVED);
        inv.setDiningTable(table);
        inv.setReservation(res);
        Payment p = payment("pay1", inv, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        stubWebhook(inv, p);

        service.handleSePayWebhook(webhook(777, "in", 100_000));

        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(p.getSepayTransactionId()).isEqualTo(777L);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(table.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(res.getReservationStatus()).isEqualTo(ReservationStatus.COMPLETED);
        verify(loyaltyService).awardPoints(inv);
    }

    @Test
    void webhook_overpaid_stillAccepted() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        stubWebhook(inv, p);

        service.handleSePayWebhook(webhook(1, "in", 150_000));

        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void webhook_invoiceAlreadyPaid_noDoublePoints() {
        Invoice inv = invoice("inv1", InvoiceStatus.PAID, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        stubWebhook(inv, p);

        service.handleSePayWebhook(webhook(1, "in", 100_000));

        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(loyaltyService, never()).awardPoints(any());
        verify(voucherService, never()).consume(any());
    }

    @Test
    void webhook_cashPayment_cannotBeSettledByTransfer() {
        Invoice inv = invoice("inv1", InvoiceStatus.PENDING, 100_000);
        Payment p = payment("pay1", inv, PaymentMethod.CASH, PaymentStatus.PENDING);
        stubWebhook(inv, p);

        service.handleSePayWebhook(webhook(1, "in", 100_000));

        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.PENDING);
    }
}
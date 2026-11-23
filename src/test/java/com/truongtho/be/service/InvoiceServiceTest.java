package com.truongtho.be.service;

import com.truongtho.be.dto.request.InvoiceDetailRequest;
import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.entity.InvoiceDetail;
import com.truongtho.be.entity.InvoiceDetailId;
import com.truongtho.be.entity.Payment;
import com.truongtho.be.entity.Product;
import com.truongtho.be.entity.Reservation;
import com.truongtho.be.entity.Voucher;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.enums.PaymentStatus;
import com.truongtho.be.enums.ProductStatus;
import com.truongtho.be.enums.ReservationStatus;
import com.truongtho.be.enums.TableStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.InvoiceMapper;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.DiningTableRepository;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.InvoiceRepository;
import com.truongtho.be.repository.PaymentRepository;
import com.truongtho.be.repository.ProductRepository;
import com.truongtho.be.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock ProductRepository productRepository;
    @Mock DiningTableRepository diningTableRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock InvoiceMapper invoiceMapper;
    @Mock CustomerRepository customerRepository;
    @Mock LoyaltyService loyaltyService;
    @Mock VoucherService voucherService;
    @Mock PaymentRepository paymentRepository;
    @InjectMocks InvoiceService service;

    @BeforeEach
    void defaults() {
        lenient().when(voucherService.calculateDiscount(any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(loyaltyService.pointsValue(anyInt()))
                .thenAnswer(i -> BigDecimal.valueOf(100L * i.<Integer>getArgument(0)));
    }



    private Invoice openInvoice(String id) {
        Invoice i = new Invoice();
        i.setId(id);
        i.setInvoiceStatus(InvoiceStatus.PENDING);
        i.setDiscountAmount(BigDecimal.ZERO);
        i.setTotalAmount(BigDecimal.ZERO);
        return i;
    }

    private Product product(long id, long price) {
        Product p = new Product();
        p.setId(id);
        p.setUnitPrice(BigDecimal.valueOf(price));
        p.setProductStatus(ProductStatus.AVAILABLE);
        return p;
    }

    private InvoiceDetail line(Invoice inv, Product p, int qty) {
        InvoiceDetail d = new InvoiceDetail();
        d.setInvoiceDetailId(new InvoiceDetailId(inv.getId(), p.getId()));
        d.setInvoice(inv);
        d.setProduct(p);
        d.setQuantity(qty);
        d.setUnitPrice(p.getUnitPrice());
        d.setTotal(p.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        inv.getInvoiceDetails().add(d);
        return d;
    }

    private DiningTable table(long id, TableStatus status) {
        DiningTable t = new DiningTable();
        t.setId(id);
        t.setTableStatus(status);
        return t;
    }

    private void invoiceFound(Invoice i) {
        when(invoiceRepository.findByIdForUpdate(i.getId())).thenReturn(Optional.of(i));
    }


    @Test
    void createInvoice_tableNotFound_throws() {
        when(diningTableRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createInvoice(5L, "emp1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createInvoice_tableAlreadyHasOpenInvoice_throws() {
        when(diningTableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table(5L, TableStatus.AVAILABLE)));
        when(employeeRepository.findById("emp1")).thenReturn(Optional.of(mock(Employee.class)));
        when(invoiceRepository.existsByDiningTable_IdAndInvoiceStatus(5L, InvoiceStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.createInvoice(5L, "emp1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang mở");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoice_tableInUse_throws() {
        when(diningTableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(table(5L, TableStatus.MAINTENANCE)));
        when(employeeRepository.findById("emp1")).thenReturn(Optional.of(mock(Employee.class)));

        assertThatThrownBy(() -> service.createInvoice(5L, "emp1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không ở trạng thái trống");
    }

    @Test
    void createInvoice_availableTable_startsServing() {
        DiningTable t = table(5L, TableStatus.AVAILABLE);
        when(diningTableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(t));
        when(employeeRepository.findById("emp1")).thenReturn(Optional.of(mock(Employee.class)));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createInvoice(5L, "emp1");

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(saved.capture());
        assertThat(saved.getValue().getInvoiceStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("0");
        assertThat(t.getTableStatus()).isEqualTo(TableStatus.SERVING);
    }

    @Test
    void createInvoice_reservedTable_linksReservationAndCustomer() {
        DiningTable t = table(5L, TableStatus.RESERVED);
        Reservation r = new Reservation();
        Customer c = new Customer();
        c.setId("u1");
        r.setCustomer(c);
        when(diningTableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(t));
        when(employeeRepository.findById("emp1")).thenReturn(Optional.of(mock(Employee.class)));
        when(reservationRepository
                .findFirstByDiningTable_IdAndReservationStatusOrderByReservationTimeDesc(5L, ReservationStatus.ARRIVED))
                .thenReturn(Optional.of(r));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createInvoice(5L, "emp1");

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(saved.capture());
        assertThat(saved.getValue().getReservation()).isSameAs(r);
        assertThat(saved.getValue().getCustomer()).isSameAs(c);
    }


    @Test
    void addDetail_newProduct_computesLineAndTotal() {
        Invoice inv = openInvoice("inv1");
        invoiceFound(inv);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, 50_000)));

        service.addDetail("inv1", new InvoiceDetailRequest(1L, 2));

        assertThat(inv.getInvoiceDetails()).hasSize(1);
        assertThat(inv.getInvoiceDetails().iterator().next().getTotal()).isEqualByComparingTo("100000");
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("100000");
        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void addDetail_sameProductTwice_accumulatesQuantity() {
        Invoice inv = openInvoice("inv1");
        invoiceFound(inv);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, 50_000)));

        service.addDetail("inv1", new InvoiceDetailRequest(1L, 2));
        service.addDetail("inv1", new InvoiceDetailRequest(1L, 1));

        assertThat(inv.getInvoiceDetails()).hasSize(1);
        assertThat(inv.getInvoiceDetails().iterator().next().getQuantity()).isEqualTo(3);
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("150000");
    }

    @Test
    void addDetail_outOfStock_throws() {
        Invoice inv = openInvoice("inv1");
        invoiceFound(inv);
        Product p = product(1L, 50_000);
        p.setProductStatus(ProductStatus.OUT_OF_STOCK);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.addDetail("inv1", new InvoiceDetailRequest(1L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thể bán");
        assertThat(inv.getInvoiceDetails()).isEmpty();
    }

    @Test
    void addDetail_zeroQuantity_throws() {
        invoiceFound(openInvoice("inv1"));

        assertThatThrownBy(() -> service.addDetail("inv1", new InvoiceDetailRequest(1L, 0)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addDetail_invoiceAlreadyPaid_throws() {
        Invoice inv = openInvoice("inv1");
        inv.setInvoiceStatus(InvoiceStatus.PAID);
        invoiceFound(inv);

        assertThatThrownBy(() -> service.addDetail("inv1", new InvoiceDetailRequest(1L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang mở");
    }

    @Test
    void updateDetail_changesQuantityAndTotal() {
        Invoice inv = openInvoice("inv1");
        InvoiceDetail d = line(inv, product(1L, 50_000), 1);
        invoiceFound(inv);

        service.updateDetail("inv1", 1L, 3);

        assertThat(d.getQuantity()).isEqualTo(3);
        assertThat(d.getTotal()).isEqualByComparingTo("150000");
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("150000");
    }

    @Test
    void removeDetail_lowersTotal() {
        Invoice inv = openInvoice("inv1");
        line(inv, product(1L, 50_000), 2);
        line(inv, product(2L, 30_000), 1);
        invoiceFound(inv);

        service.removeDetail("inv1", 1L);

        assertThat(inv.getInvoiceDetails()).hasSize(1);
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void removeDetail_unknownProduct_throws() {
        Invoice inv = openInvoice("inv1");
        line(inv, product(1L, 50_000), 1);
        invoiceFound(inv);

        assertThatThrownBy(() -> service.removeDetail("inv1", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recalc_voucherBelowMinimum_isRemoved() {
        Invoice inv = openInvoice("inv1");
        Voucher v = new Voucher();
        inv.setVoucher(v);
        line(inv, product(1L, 50_000), 1);
        line(inv, product(2L, 30_000), 1);
        invoiceFound(inv);
        when(voucherService.isBelowMinimum(eq(v), any())).thenReturn(true);

        service.removeDetail("inv1", 1L);

        assertThat(inv.getVoucher()).isNull();
    }

    @Test
    void recalc_discountNeverExceedsSubtotal() {
        Invoice inv = openInvoice("inv1");
        Voucher v = new Voucher();
        inv.setVoucher(v);
        line(inv, product(1L, 50_000), 1);
        invoiceFound(inv);
        when(voucherService.calculateDiscount(eq(v), any())).thenReturn(BigDecimal.valueOf(150_000));

        service.updateDetail("inv1", 1L, 2);

        assertThat(inv.getDiscountAmount()).isEqualByComparingTo("100000");
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void recalc_pointsNoLongerAllowed_areRefunded() {
        Invoice inv = openInvoice("inv1");
        inv.setPointsRedeemed(100);
        line(inv, product(1L, 50_000), 1);
        invoiceFound(inv);
        when(loyaltyService.maxRedeemablePoints(any())).thenReturn(10);

        service.updateDetail("inv1", 1L, 2);

        verify(loyaltyService).refund(inv);
        assertThat(inv.getTotalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void recalc_totalChanged_voidsPendingPayments() {
        Invoice inv = openInvoice("inv1");
        invoiceFound(inv);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, 50_000)));
        Payment pending = new Payment();
        pending.setPaymentStatus(PaymentStatus.PENDING);
        when(paymentRepository.findAllByInvoice_IdAndPaymentStatus("inv1", PaymentStatus.PENDING))
                .thenReturn(List.of(pending));

        service.addDetail("inv1", new InvoiceDetailRequest(1L, 2));

        assertThat(pending.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void recalc_totalUnchanged_keepsPendingPayments() {
        Invoice inv = openInvoice("inv1");
        line(inv, product(1L, 50_000), 2);
        inv.setTotalAmount(BigDecimal.valueOf(100_000));
        invoiceFound(inv);

        service.updateDetail("inv1", 1L, 2);

        verify(paymentRepository, never()).findAllByInvoice_IdAndPaymentStatus(any(), any());
    }



    @Test
    void cancelInvoice_refundsPointsAndFreesTable() {
        Invoice inv = openInvoice("inv1");
        DiningTable t = table(5L, TableStatus.SERVING);
        inv.setDiningTable(t);
        invoiceFound(inv);

        service.cancelInvoice("inv1");

        verify(loyaltyService).refund(inv);
        assertThat(inv.getInvoiceStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(t.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    void cancelInvoice_alreadyPaid_throws() {
        Invoice inv = openInvoice("inv1");
        inv.setInvoiceStatus(InvoiceStatus.PAID);
        invoiceFound(inv);

        assertThatThrownBy(() -> service.cancelInvoice("inv1"))
                .isInstanceOf(BusinessException.class);
        verify(loyaltyService, never()).refund(any());
    }



    @Test
    void redeemPoints_refundsFirstThenRedeems() {
        Invoice inv = openInvoice("inv1");
        line(inv, product(1L, 50_000), 2);
        invoiceFound(inv);

        service.redeemPoints("inv1", 50);

        InOrder order = inOrder(loyaltyService);
        order.verify(loyaltyService).refund(inv);
        order.verify(loyaltyService).redeem(eq(inv), eq(50),
                argThat((BigDecimal base) -> base.compareTo(BigDecimal.valueOf(100_000)) == 0));
    }

    // ---------- moveTable ----------

    @Test
    void moveTable_sameTable_throws() {
        Invoice inv = openInvoice("inv1");
        inv.setDiningTable(table(1L, TableStatus.SERVING));
        invoiceFound(inv);

        assertThatThrownBy(() -> service.moveTable("inv1", 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void moveTable_targetOccupied_throws() {
        Invoice inv = openInvoice("inv1");
        inv.setDiningTable(table(1L, TableStatus.SERVING));
        invoiceFound(inv);
        when(diningTableRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(table(2L, TableStatus.SERVING)));

        assertThatThrownBy(() -> service.moveTable("inv1", 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không trống");
    }

    @Test
    void moveTable_success_swapsTables() {
        Invoice inv = openInvoice("inv1");
        DiningTable from = table(1L, TableStatus.SERVING);
        DiningTable to = table(2L, TableStatus.AVAILABLE);
        Reservation res = new Reservation();
        res.setReservationStatus(ReservationStatus.ARRIVED);
        res.setDiningTable(from);
        inv.setDiningTable(from);
        inv.setReservation(res);
        invoiceFound(inv);
        when(diningTableRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(to));

        service.moveTable("inv1", 2L);

        assertThat(to.getTableStatus()).isEqualTo(TableStatus.SERVING);
        assertThat(from.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(inv.getDiningTable()).isSameAs(to);
        assertThat(res.getDiningTable()).isSameAs(to);
    }



    @Test
    void mergeTable_withItself_throws() {
        Invoice target = openInvoice("tgt");
        target.setDiningTable(table(10L, TableStatus.SERVING));
        invoiceFound(target);

        assertThatThrownBy(() -> service.mergeTable("tgt", 10L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mergeTable_combinesLinesAndClosesSource() {
        Product a = product(1L, 50_000);
        Product b = product(2L, 30_000);
        Invoice target = openInvoice("tgt");
        target.setDiningTable(table(10L, TableStatus.SERVING));
        line(target, a, 1);
        Invoice source = openInvoice("src");
        DiningTable sourceTable = table(20L, TableStatus.SERVING);
        source.setDiningTable(sourceTable);
        line(source, a, 2);
        line(source, b, 1);
        invoiceFound(target);
        invoiceFound(source);
        when(invoiceRepository.findFirstByDiningTable_IdAndInvoiceStatusOrderByIssuedAtDesc(20L, InvoiceStatus.PENDING))
                .thenReturn(Optional.of(source));

        service.mergeTable("tgt", 20L);

        assertThat(target.getInvoiceDetails()).hasSize(2);
        assertThat(target.getTotalAmount()).isEqualByComparingTo("180000");
        assertThat(source.getInvoiceStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(source.getInvoiceDetails()).isEmpty();
        assertThat(sourceTable.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        verify(loyaltyService).refund(source);
    }
}
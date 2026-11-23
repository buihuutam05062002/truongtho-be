package com.truongtho.be.service;

import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock CustomerRepository customerRepository;
    @InjectMocks LoyaltyService service;


    private Customer customer(String id, int points) {
        Customer c = new Customer();
        c.setId(id);
        c.setPoints(points);
        return c;
    }

    private Invoice invoiceFor(Customer c, long total) {
        Invoice i = new Invoice();
        i.setCustomer(c);
        i.setTotalAmount(BigDecimal.valueOf(total));
        return i;
    }

    private void locked(Customer c) {
        when(customerRepository.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
    }


    @Test
    void maxRedeemable_isHalfOfBase() {

        assertThat(service.maxRedeemablePoints(BigDecimal.valueOf(100_000))).isEqualTo(500);
        assertThat(service.maxRedeemablePoints(BigDecimal.valueOf(20_000))).isEqualTo(100);
    }

    @Test
    void maxRedeemable_roundsDown() {
        assertThat(service.maxRedeemablePoints(BigDecimal.valueOf(99))).isEqualTo(0);
        assertThat(service.maxRedeemablePoints(BigDecimal.valueOf(299))).isEqualTo(1);  // 149,5đ -> 1 điểm
    }

    @Test
    void maxRedeemable_zeroOrNegativeBase_isZero() {
        assertThat(service.maxRedeemablePoints(BigDecimal.ZERO)).isEqualTo(0);
        assertThat(service.maxRedeemablePoints(BigDecimal.valueOf(-5_000))).isEqualTo(0);
    }

    @Test
    void maxRedeemable_nullBase_isZero() {
        assertThat(service.maxRedeemablePoints(null)).isEqualTo(0);
    }

    @Test
    void pointsValue_is100dPerPoint() {
        assertThat(service.pointsValue(500)).isEqualByComparingTo("50000");
    }


    @Test
    void award_guestInvoice_doesNothing() {
        Invoice guest = new Invoice();
        guest.setTotalAmount(BigDecimal.valueOf(500_000));

        service.awardPoints(guest);

        verifyNoInteractions(customerRepository);
    }

    @Test
    void award_roundsDown_andAddsToTotalSpent() {
        Customer c = customer("u1", 5);
        c.setTotalSpent(BigDecimal.valueOf(1_000_000));
        locked(c);

        service.awardPoints(invoiceFor(c, 125_000));

        assertThat(c.getPoints()).isEqualTo(17);
        assertThat(c.getTotalSpent()).isEqualByComparingTo("1125000");
    }

    @Test
    void award_underOnePointThreshold_stillCountsSpending() {
        Customer c = customer("u1", 5);
        locked(c);

        service.awardPoints(invoiceFor(c, 9_999));

        assertThat(c.getPoints()).isEqualTo(5);
        assertThat(c.getTotalSpent()).isEqualByComparingTo("9999");
    }

    @Test
    void award_nullBalances_treatedAsZero() {
        Customer c = new Customer();
        c.setId("u1");
        c.setPoints(null);
        c.setTotalSpent(null);
        locked(c);

        service.awardPoints(invoiceFor(c, 50_000));

        assertThat(c.getPoints()).isEqualTo(5);
        assertThat(c.getTotalSpent()).isEqualByComparingTo("50000");
    }


    @Test
    void redeem_guestInvoice_throws() {
        Invoice guest = new Invoice();

        assertThatThrownBy(() -> service.redeem(guest, 10, BigDecimal.valueOf(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("gắn khách hàng");
    }

    @Test
    void redeem_zeroPoints_isNoOp() {
        Customer c = customer("u1", 300);
        Invoice inv = invoiceFor(c, 100_000);

        service.redeem(inv, 0, BigDecimal.valueOf(100_000));

        verifyNoInteractions(customerRepository);
        assertThat(inv.getPointsRedeemed()).isEqualTo(0);
    }

    @Test
    void redeem_success_deductsBalanceAndRecordsOnInvoice() {
        Customer c = customer("u1", 300);
        locked(c);
        Invoice inv = invoiceFor(c, 100_000);

        service.redeem(inv, 200, BigDecimal.valueOf(100_000));

        assertThat(c.getPoints()).isEqualTo(100);
        assertThat(inv.getPointsRedeemed()).isEqualTo(200);
    }

    @Test
    void redeem_moreThanBalance_throwsAndKeepsBalance() {
        Customer c = customer("u1", 100);
        locked(c);
        Invoice inv = invoiceFor(c, 100_000);

        assertThatThrownBy(() -> service.redeem(inv, 200, BigDecimal.valueOf(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("còn 100 điểm");
        assertThat(c.getPoints()).isEqualTo(100);
    }

    @Test
    void redeem_moreThanHalfOfBill_throws() {
        Customer c = customer("u1", 1_000);
        locked(c);
        Invoice inv = invoiceFor(c, 100_000);

        assertThatThrownBy(() -> service.redeem(inv, 501, BigDecimal.valueOf(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tối đa 500 điểm");
        assertThat(c.getPoints()).isEqualTo(1_000);
    }


    @Test
    void refund_nothingRedeemed_doesNothing() {
        Customer c = customer("u1", 100);
        Invoice inv = invoiceFor(c, 100_000);
        inv.setPointsRedeemed(null);

        service.refund(inv);

        verifyNoInteractions(customerRepository);
    }

    @Test
    void refund_noCustomer_doesNothing() {
        Invoice inv = new Invoice();
        inv.setPointsRedeemed(50);

        service.refund(inv);

        verifyNoInteractions(customerRepository);
    }

    @Test
    void refund_givesPointsBackAndResetsInvoice() {
        Customer c = customer("u1", 100);
        locked(c);
        Invoice inv = invoiceFor(c, 100_000);
        inv.setPointsRedeemed(200);

        service.refund(inv);

        assertThat(c.getPoints()).isEqualTo(300);
        assertThat(inv.getPointsRedeemed()).isEqualTo(0);
    }
}
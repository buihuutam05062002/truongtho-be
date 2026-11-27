package com.truongtho.be.service;

import com.truongtho.be.entity.Voucher;
import com.truongtho.be.enums.DiscountType;
import com.truongtho.be.enums.VoucherStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.mapper.VoucherMapper;
import com.truongtho.be.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock VoucherRepository voucherRepository;
    @Mock VoucherMapper voucherMapper;
    @InjectMocks VoucherService service;


    private BigDecimal vnd(long n) {
        return BigDecimal.valueOf(n);
    }

    private Voucher voucher(DiscountType type, long value) {
        Voucher v = new Voucher();
        v.setDiscountType(type);
        v.setDiscountValue(vnd(value));
        v.setVoucherStatus(VoucherStatus.ACTIVE);
        return v;
    }

    private void codeExists(String code, Voucher v) {
        when(voucherRepository.findByCodeIgnoreCase(code)).thenReturn(Optional.of(v));
    }


    @Test
    void discount_nullVoucher_isZero() {
        assertThat(service.calculateDiscount(null, vnd(200_000))).isEqualByComparingTo("0");
    }

    @Test
    void discount_percentage() {
        Voucher v = voucher(DiscountType.PERCENTAGE, 10);
        assertThat(service.calculateDiscount(v, vnd(200_000))).isEqualByComparingTo("20000");
    }

    @Test
    void discount_percentage_cappedByMax() {
        Voucher v = voucher(DiscountType.PERCENTAGE, 50);
        v.setDiscountValueMax(vnd(30_000));
        assertThat(service.calculateDiscount(v, vnd(200_000))).isEqualByComparingTo("30000");
    }

    @Test
    void discount_percentage_roundsHalfUp() {
        Voucher v = voucher(DiscountType.PERCENTAGE, 10);
        assertThat(service.calculateDiscount(v, vnd(10_005))).isEqualByComparingTo("1001"); // 1000,5 -> 1001
    }

    @Test
    void discount_percentage_zeroMaxMeansNoCap() {
        Voucher v = voucher(DiscountType.PERCENTAGE, 50);
        v.setDiscountValueMax(BigDecimal.ZERO);
        assertThat(service.calculateDiscount(v, vnd(200_000))).isEqualByComparingTo("100000");
    }

    @Test
    void discount_fixedAmount() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 50_000);
        assertThat(service.calculateDiscount(v, vnd(200_000))).isEqualByComparingTo("50000");
    }

    @Test
    void discount_fixedAmount_neverExceedsSubtotal() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 50_000);
        assertThat(service.calculateDiscount(v, vnd(30_000))).isEqualByComparingTo("30000");
    }

    @Test
    void discount_negativeValue_isZero() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, -5_000);
        assertThat(service.calculateDiscount(v, vnd(200_000))).isEqualByComparingTo("0");
    }

    @Test
    void findUsable_codeNotFound_throws() {
        when(voucherRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findUsable("NOPE", vnd(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    void findUsable_notActive_throws() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setVoucherStatus(VoucherStatus.SUSPENDED);
        codeExists("SALE", v);

        assertThatThrownBy(() -> service.findUsable("SALE", vnd(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hoạt động");
    }

    @Test
    void findUsable_notStartedYet_throws() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setStart(LocalDateTime.now().plusDays(1));
        codeExists("SALE", v);

        assertThatThrownBy(() -> service.findUsable("SALE", vnd(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa đến");
    }

    @Test
    void findUsable_expired_throws() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setExpire(LocalDateTime.now().minusDays(1));
        codeExists("SALE", v);

        assertThatThrownBy(() -> service.findUsable("SALE", vnd(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    void findUsable_usesExhausted_throws() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setMaxQuantity(5);
        v.setUsedQuantity(5);
        codeExists("SALE", v);

        assertThatThrownBy(() -> service.findUsable("SALE", vnd(100_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hết lượt");
    }

    @Test
    void findUsable_belowMinimum_throws() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setMinimumOrderAmount(vnd(100_000));
        codeExists("SALE", v);

        assertThatThrownBy(() -> service.findUsable("SALE", vnd(99_999)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tối thiểu");
    }

    @Test
    void findUsable_valid_trimsCode() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        codeExists("SALE10", v);

        assertThat(service.findUsable("  SALE10 ", vnd(100_000))).isSameAs(v);
    }

    @Test
    void findUsable_valid_exactlyAtMinimum() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setMinimumOrderAmount(vnd(100_000));
        codeExists("SALE", v);

        assertThat(service.findUsable("SALE", vnd(100_000))).isSameAs(v);
    }

    @Test
    void findUsable_valid_unlimitedUses() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 10_000);
        v.setMaxQuantity(null);
        v.setUsedQuantity(999);
        codeExists("SALE", v);

        assertThat(service.findUsable("SALE", vnd(100_000))).isSameAs(v);
    }

    @Test
    void isBelowMinimum_noMinimum_false() {
        assertThat(service.isBelowMinimum(voucher(DiscountType.FIXED_AMOUNT, 1), vnd(1))).isFalse();
    }

    @Test
    void isBelowMinimum_belowMinimum_true() {
        Voucher v = voucher(DiscountType.FIXED_AMOUNT, 1);
        v.setMinimumOrderAmount(vnd(100_000));
        assertThat(service.isBelowMinimum(v, vnd(99_999))).isTrue();
    }

    @Test
    void consume_null_doesNothing() {
        service.consume(null);

        verifyNoInteractions(voucherRepository);
    }

    @Test
    void consume_incrementsFromNull() {
        Voucher v = new Voucher();
        v.setId(1L);
        v.setUsedQuantity(null);
        when(voucherRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(v));

        service.consume(v);

        assertThat(v.getUsedQuantity()).isEqualTo(1);
    }

    @Test
    void consume_incrementsExistingCount() {
        Voucher v = new Voucher();
        v.setId(1L);
        v.setUsedQuantity(4);
        when(voucherRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(v));

        service.consume(v);

        assertThat(v.getUsedQuantity()).isEqualTo(5);
    }

    @Test
    void consume_unknownVoucher_isIgnored() {
        Voucher v = new Voucher();
        v.setId(1L);
        when(voucherRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        service.consume(v);
    }
}
package com.truongtho.be.service;

import com.truongtho.be.dto.response.LoyaltyResponse;
import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.repository.CustomerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoyaltyService {

    public static final int VND_PER_POINT_EARNED = 10_000;
    public static final int VND_PER_POINT_REDEEMED = 100;
    public static final int MAX_REDEEM_PERCENT = 50;

    CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public LoyaltyResponse getMine(String userId) {
        Customer c = customerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
        return new LoyaltyResponse(nz(c.getPoints()), nz(c.getTotalSpent()),
                VND_PER_POINT_EARNED, VND_PER_POINT_REDEEMED, MAX_REDEEM_PERCENT);
    }

    public void awardPoints(Invoice invoice) {
        if (invoice.getCustomer() == null) return;          // walk-in guest: nothing to credit
        BigDecimal paid = nz(invoice.getTotalAmount());
        int earned = paid.divide(BigDecimal.valueOf(VND_PER_POINT_EARNED), 0, RoundingMode.DOWN).intValue();

        Customer c = lock(invoice.getCustomer().getId());
        c.setPoints(nz(c.getPoints()) + earned);
        c.setTotalSpent(nz(c.getTotalSpent()).add(paid));
    }

    public void redeem(Invoice invoice, int points, BigDecimal subtotal) {
        if (invoice.getCustomer() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Hãy gắn khách hàng vào hóa đơn trước");
        }
        if (points <= 0) return;

        int cap = maxRedeemablePoints(subtotal);

        Customer c = lock(invoice.getCustomer().getId());
        int balance = nz(c.getPoints());
        if (points > balance) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Khách chỉ còn " + balance + " điểm");
        }
        if (points > cap) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Hóa đơn này chỉ được dùng tối đa " + cap + " điểm (" + MAX_REDEEM_PERCENT + "% tạm tính)");
        }
        c.setPoints(balance - points);
        invoice.setPointsRedeemed(points);
    }

    public int maxRedeemablePoints(BigDecimal base) {
        if (base == null || base.signum() <= 0) return 0;
        return base.multiply(BigDecimal.valueOf(MAX_REDEEM_PERCENT))
                .divide(BigDecimal.valueOf(100L * VND_PER_POINT_REDEEMED), 0, RoundingMode.DOWN)
                .intValue();
    }

    public BigDecimal pointsValue(int points) {
        return valueOf(points);
    }

    public void refund(Invoice invoice) {
        int used = nz(invoice.getPointsRedeemed());
        if (used == 0 || invoice.getCustomer() == null) return;

        Customer c = lock(invoice.getCustomer().getId());
        c.setPoints(nz(c.getPoints()) + used);
        invoice.setPointsRedeemed(0);
    }

    private Customer lock(String customerId) {
        return customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + customerId));
    }

    private static BigDecimal valueOf(int points) {
        return BigDecimal.valueOf((long) points * VND_PER_POINT_REDEEMED);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

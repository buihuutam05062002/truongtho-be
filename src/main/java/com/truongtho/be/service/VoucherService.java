package com.truongtho.be.service;

import com.truongtho.be.dto.request.VoucherRequest;
import com.truongtho.be.dto.response.VoucherResponse;
import com.truongtho.be.entity.Voucher;
import com.truongtho.be.enums.DiscountType;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.enums.VoucherStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.mapper.VoucherMapper;
import com.truongtho.be.repository.VoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE)
public class VoucherService {
    final VoucherRepository voucherRepository;
    final VoucherMapper voucherMapper;


    public Voucher findById(Long idx){
        return  voucherRepository.findById(idx).orElseThrow();
    }
    public VoucherResponse delete(Long idx){
        Voucher voucher = findById(idx);
        voucherRepository.delete(voucher);
        return  voucherMapper.toResponse(voucher);
    }

    public VoucherResponse save(VoucherRequest voucherRequest){
        Voucher voucher = voucherMapper.toEntity(voucherRequest);
        voucherRepository.save(voucher);
        return voucherMapper.toResponse(voucher);
    }

    public VoucherResponse update(Long idx,VoucherRequest voucherRequest){
        Voucher voucher = findById(idx);
        voucher = voucherMapper.toEntity(voucherRequest);
        voucherRepository.save(voucher);
        return voucherMapper.toResponse(voucher);
    }


    public Voucher findUsable(String code, BigDecimal subtotal) {
        Voucher v = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Mã giảm giá không tồn tại"));

        if (v.getVoucherStatus() != VoucherStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mã giảm giá hiện không hoạt động");
        }
        LocalDateTime now = LocalDateTime.now();
        if (v.getStart() != null && now.isBefore(v.getStart())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (v.getExpire() != null && now.isAfter(v.getExpire())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mã giảm giá đã hết hạn");
        }
        if (v.getMaxQuantity() != null && used(v) >= v.getMaxQuantity()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mã giảm giá đã hết lượt sử dụng");
        }
        if (isBelowMinimum(v, subtotal)) {
            String min = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(v.getMinimumOrderAmount());
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Hóa đơn cần tối thiểu " + min + " ₫ để dùng mã này");
        }
        return v;
    }

    public boolean isBelowMinimum(Voucher v, BigDecimal subtotal) {
        return v.getMinimumOrderAmount() != null && subtotal.compareTo(v.getMinimumOrderAmount()) < 0;
    }

    public BigDecimal calculateDiscount(Voucher v, BigDecimal subtotal) {
        if (v == null || v.getDiscountValue() == null || v.getDiscountType() == null) return BigDecimal.ZERO;

        BigDecimal discount;
        if (v.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(v.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (v.getDiscountValueMax() != null && v.getDiscountValueMax().signum() > 0) {
                discount = discount.min(v.getDiscountValueMax());   // cap for percentage vouchers
            }
        } else {
            discount = v.getDiscountValue();
        }
        return discount.max(BigDecimal.ZERO).min(subtotal);
    }

    public void consume(Voucher voucher) {
        if (voucher == null) return;
        Voucher v = voucherRepository.findByIdForUpdate(voucher.getId()).orElse(null);
        if (v != null) v.setUsedQuantity(used(v) + 1);
    }

    private static int used(Voucher v) {
        return v.getUsedQuantity() == null ? 0 : v.getUsedQuantity();
    }
}

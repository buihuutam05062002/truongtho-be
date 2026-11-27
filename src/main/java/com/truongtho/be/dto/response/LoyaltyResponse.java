package com.truongtho.be.dto.response;

import java.math.BigDecimal;

public record LoyaltyResponse(
        int points,
        BigDecimal totalSpent,
        int vndPerPointEarned,
        int vndPerPointRedeemed,
        int maxRedeemPercent) {
}

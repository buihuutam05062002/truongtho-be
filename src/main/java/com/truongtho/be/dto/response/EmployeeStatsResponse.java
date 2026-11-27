package com.truongtho.be.dto.response;

public record EmployeeStatsResponse(long total, long active, long onLeave, long suspended, long resigned) {
}

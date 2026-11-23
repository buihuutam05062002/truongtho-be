package com.truongtho.be.dto.response;

import java.util.List;

public record ProductStatsResponse(
        long total,
        long orderable,
        long paused,
        List<CategoryCount> categories) {

    public record CategoryCount(Long categoryId, long count) {
    }
}

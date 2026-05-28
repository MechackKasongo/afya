package com.afya.platform.bff.dto;

import java.util.List;

public record PageUrgenceCompatResponse(
        boolean scopeRestricted,
        List<UrgenceCompatResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}

package com.afya.platform.catalog.dto;

public record BedResponse(
        Long id,
        Long hospitalServiceId,
        String label,
        boolean occupied
) {
}

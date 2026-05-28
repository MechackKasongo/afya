package com.afya.platform.bff.dto;

public record BedResponse(
        Long id,
        Long hospitalServiceId,
        String label,
        boolean occupied
) {
}

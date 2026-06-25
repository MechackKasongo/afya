package com.afya.platform.hospital.dto;

public record BedResponse(
        Long id,
        Long hospitalServiceId,
        String label,
        boolean occupied
) {
}

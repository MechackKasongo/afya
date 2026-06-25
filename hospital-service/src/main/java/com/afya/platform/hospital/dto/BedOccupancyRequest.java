package com.afya.platform.hospital.dto;

import jakarta.validation.constraints.NotNull;

public record BedOccupancyRequest(
        String roomLabel,
        String bedLabel,
        @NotNull Boolean occupied
) {
}

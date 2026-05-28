package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;

public record HospitalServiceStatusRequest(@NotNull Boolean active) {
}

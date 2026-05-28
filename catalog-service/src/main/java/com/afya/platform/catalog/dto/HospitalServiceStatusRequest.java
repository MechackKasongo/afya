package com.afya.platform.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record HospitalServiceStatusRequest(
        @NotNull(message = "Le statut actif est obligatoire")
        Boolean active
) {
}

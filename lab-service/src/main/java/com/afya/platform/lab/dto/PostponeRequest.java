package com.afya.platform.lab.dto;

import jakarta.validation.constraints.Size;

/** Report (mise en attente) d'une demande d'examen : motif optionnel mais tracé. */
public record PostponeRequest(
        @Size(max = 1000) String reason
) {
}

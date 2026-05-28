package com.afya.platform.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicalNoteRequest(
        @NotBlank(message = "Le texte est obligatoire")
        @Size(max = 4000)
        String narrative
) {
}

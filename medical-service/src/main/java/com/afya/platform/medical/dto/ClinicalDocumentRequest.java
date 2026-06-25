package com.afya.platform.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicalDocumentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String contentType,
        @NotBlank @Size(max = 500) String objectStorageKey
) {
}

package com.afya.platform.medical.dto;

public record DiseaseCatalogResponse(
        Long id,
        String diseaseType,
        String label,
        int usageCount,
        boolean selectable
) {
}

package com.afya.platform.clinical.dto;

public record DiseaseCatalogResponse(
        Long id,
        String diseaseType,
        String label,
        int usageCount,
        boolean selectable
) {
}

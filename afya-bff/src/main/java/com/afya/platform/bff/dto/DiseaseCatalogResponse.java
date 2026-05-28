package com.afya.platform.bff.dto;

public record DiseaseCatalogResponse(
        Long id,
        String diseaseType,
        String label,
        int usageCount,
        boolean selectable
) {
}

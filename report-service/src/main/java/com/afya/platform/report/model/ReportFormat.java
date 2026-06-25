package com.afya.platform.report.model;

public enum ReportFormat {
    PDF,
    XLSX;

    public static ReportFormat parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Format requis : pdf ou xlsx");
        }
        return switch (raw.strip().toLowerCase()) {
            case "pdf" -> PDF;
            case "xlsx", "excel" -> XLSX;
            default -> throw new IllegalArgumentException("Format non supporté : " + raw);
        };
    }
}

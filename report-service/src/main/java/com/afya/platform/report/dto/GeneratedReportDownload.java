package com.afya.platform.report.dto;

import com.afya.platform.report.model.ReportFormat;

public record GeneratedReportDownload(
        Long id,
        String fileName,
        String contentType,
        byte[] payload
) {
    public static GeneratedReportDownload of(Long id, String fileName, ReportFormat format, byte[] payload) {
        return new GeneratedReportDownload(
                id,
                fileName,
                format == ReportFormat.PDF
                        ? "application/pdf"
                        : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                payload
        );
    }
}

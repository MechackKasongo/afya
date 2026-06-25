package com.afya.platform.report.service;

import com.afya.platform.report.dto.GeneratedReportDownload;
import com.afya.platform.report.dto.OperationalStatsResponse;
import com.afya.platform.report.model.GeneratedReport;
import com.afya.platform.report.model.ReportFormat;
import com.afya.platform.report.repository.GeneratedReportRepository;
import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class GeneratedReportService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Lubumbashi");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZONE);

    private final GeneratedReportRepository generatedReportRepository;
    private final OperationalStatsService operationalStatsService;
    private final ReportDocumentWriter reportDocumentWriter;

    public GeneratedReportService(
            GeneratedReportRepository generatedReportRepository,
            OperationalStatsService operationalStatsService,
            ReportDocumentWriter reportDocumentWriter
    ) {
        this.generatedReportRepository = generatedReportRepository;
        this.operationalStatsService = operationalStatsService;
        this.reportDocumentWriter = reportDocumentWriter;
    }

    @Transactional
    public GeneratedReportDownload exportActivity(Instant from, Instant to, ReportFormat format) {
        OperationalStatsResponse stats = operationalStatsService.operationalStats(from, to);
        byte[] payload = reportDocumentWriter.writeActivityExport(stats, format);
        String fileName = "rapport-activite-" + FILE_TS.format(Instant.now()) + extension(format);
        GeneratedReport entity = new GeneratedReport();
        entity.setReportCode("ACTIVITY_EXPORT");
        entity.setFormat(format);
        entity.setPeriodFrom(stats.activity().from());
        entity.setPeriodTo(stats.activity().to());
        entity.setFileName(fileName);
        entity.setContentType(format == ReportFormat.PDF
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        entity.setFileSize(payload.length);
        entity.setPayload(payload);
        entity.setGeneratedBy(AuditActorResolver.currentUsername());
        GeneratedReport saved = generatedReportRepository.save(entity);
        return GeneratedReportDownload.of(saved.getId(), saved.getFileName(), format, payload);
    }

    @Transactional(readOnly = true)
    public GeneratedReportDownload download(Long id) {
        GeneratedReport report = generatedReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rapport introuvable : " + id));
        return GeneratedReportDownload.of(
                report.getId(),
                report.getFileName(),
                report.getFormat(),
                report.getPayload()
        );
    }

    private static String extension(ReportFormat format) {
        return format == ReportFormat.PDF ? ".pdf" : ".xlsx";
    }
}

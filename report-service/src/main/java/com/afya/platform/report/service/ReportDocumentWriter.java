package com.afya.platform.report.service;

import com.afya.platform.report.dto.ActivityCountItem;
import com.afya.platform.report.dto.ActivityReportResponse;
import com.afya.platform.report.dto.LabStatsResponse;
import com.afya.platform.report.dto.NursingStatsResponse;
import com.afya.platform.report.dto.OperationalStatsResponse;
import com.afya.platform.report.model.ReportFormat;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReportDocumentWriter {

    private static final ZoneId ZONE = ZoneId.of("Africa/Lubumbashi");
    private static final DateTimeFormatter DISPLAY_TS =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE);

    public byte[] writeActivityExport(OperationalStatsResponse stats, ReportFormat format) {
        return switch (format) {
            case PDF -> writePdf(stats);
            case XLSX -> writeExcel(stats);
        };
    }

    private byte[] writePdf(OperationalStatsResponse stats) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Rapport d'activité — Afya Platform", titleFont));
            document.add(new Paragraph(
                    "Période : " + DISPLAY_TS.format(stats.activity().from())
                            + " → " + DISPLAY_TS.format(stats.activity().to()),
                    bodyFont));
            document.add(new Paragraph("Événements audit : " + stats.activity().totalEvents(), bodyFont));
            if (stats.activity().degraded()) {
                document.add(new Paragraph("Note audit : " + stats.activity().notice(), bodyFont));
            }
            document.add(new Paragraph(" "));

            addPdfSection(document, "Actions", stats.activity().byAction(), sectionFont, bodyFont);
            addPdfSection(document, "Services source", stats.activity().bySourceService(), sectionFont, bodyFont);
            addPdfLabSection(document, stats.lab(), sectionFont, bodyFont);
            addPdfNursingSection(document, stats.nursing(), sectionFont, bodyFont);

            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Génération PDF impossible", ex);
        }
    }

    private void addPdfSection(
            Document document,
            String title,
            List<ActivityCountItem> items,
            Font sectionFont,
            Font bodyFont
    ) throws DocumentException {
        document.add(new Paragraph(title, sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Libellé", bodyFont));
        table.addCell(headerCell("Total", bodyFont));
        for (ActivityCountItem item : items) {
            table.addCell(bodyCell(item.key(), bodyFont));
            table.addCell(bodyCell(String.valueOf(item.count()), bodyFont));
        }
        if (items.isEmpty()) {
            table.addCell(bodyCell("—", bodyFont));
            table.addCell(bodyCell("0", bodyFont));
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addPdfLabSection(Document document, LabStatsResponse lab, Font sectionFont, Font bodyFont)
            throws DocumentException {
        document.add(new Paragraph("Statistiques laboratoire", sectionFont));
        if (lab.degraded()) {
            document.add(new Paragraph(lab.notice(), bodyFont));
        }
        document.add(new Paragraph("Demandes d'examens : " + lab.examRequests(), bodyFont));
        document.add(new Paragraph("En attente : " + lab.pendingRequests(), bodyFont));
        document.add(new Paragraph("Prélèvements : " + lab.specimenCollected(), bodyFont));
        document.add(new Paragraph("Résultats disponibles : " + lab.resultsAvailable(), bodyFont));
        document.add(new Paragraph("Paramètres anormaux : " + lab.abnormalParameters(), bodyFont));
        document.add(new Paragraph(" "));
    }

    private void addPdfNursingSection(
            Document document,
            NursingStatsResponse nursing,
            Font sectionFont,
            Font bodyFont
    ) throws DocumentException {
        document.add(new Paragraph("Statistiques soins infirmiers", sectionFont));
        if (nursing.degraded()) {
            document.add(new Paragraph(nursing.notice(), bodyFont));
        }
        document.add(new Paragraph("Relevés de constantes : " + nursing.vitalSignReadings(), bodyFont));
        document.add(new Paragraph("Alertes constantes : " + nursing.vitalSignAlerts(), bodyFont));
        document.add(new Paragraph("Notifications prescription : " + nursing.prescriptionNotifications(), bodyFont));
        document.add(new Paragraph("Prescriptions exécutées : " + nursing.executedPrescriptions(), bodyFont));
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setGrayFill(0.9f);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }

    private byte[] writeExcel(OperationalStatsResponse stats) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet summary = workbook.createSheet("Résumé");
            int rowIdx = 0;
            rowIdx = writeLabelValue(summary, rowIdx, "Période début", DISPLAY_TS.format(stats.activity().from()));
            rowIdx = writeLabelValue(summary, rowIdx, "Période fin", DISPLAY_TS.format(stats.activity().to()));
            rowIdx = writeLabelValue(summary, rowIdx, "Événements audit", String.valueOf(stats.activity().totalEvents()));
            rowIdx = writeLabelValue(summary, rowIdx, "Demandes labo", String.valueOf(stats.lab().examRequests()));
            rowIdx = writeLabelValue(summary, rowIdx, "Résultats labo", String.valueOf(stats.lab().resultsAvailable()));
            rowIdx = writeLabelValue(summary, rowIdx, "Constantes vitales", String.valueOf(stats.nursing().vitalSignReadings()));
            rowIdx = writeLabelValue(summary, rowIdx, "Alertes constantes", String.valueOf(stats.nursing().vitalSignAlerts()));

            writeCountSheet(workbook.createSheet("Actions audit"), stats.activity().byAction());
            writeCountSheet(workbook.createSheet("Services audit"), stats.activity().bySourceService());
            writeCountSheet(workbook.createSheet("Acteurs audit"), stats.activity().topActors());

            Sheet labSheet = workbook.createSheet("Laboratoire");
            int labRow = 0;
            labRow = writeLabelValue(labSheet, labRow, "Demandes", String.valueOf(stats.lab().examRequests()));
            labRow = writeLabelValue(labSheet, labRow, "En attente", String.valueOf(stats.lab().pendingRequests()));
            labRow = writeLabelValue(labSheet, labRow, "Prélèvements", String.valueOf(stats.lab().specimenCollected()));
            labRow = writeLabelValue(labSheet, labRow, "Résultats", String.valueOf(stats.lab().resultsAvailable()));
            writeLabelValue(labSheet, labRow, "Anomalies", String.valueOf(stats.lab().abnormalParameters()));

            Sheet nursingSheet = workbook.createSheet("Soins");
            int nursingRow = 0;
            nursingRow = writeLabelValue(nursingSheet, nursingRow, "Constantes", String.valueOf(stats.nursing().vitalSignReadings()));
            nursingRow = writeLabelValue(nursingSheet, nursingRow, "Alertes", String.valueOf(stats.nursing().vitalSignAlerts()));
            nursingRow = writeLabelValue(nursingSheet, nursingRow, "Notifications", String.valueOf(stats.nursing().prescriptionNotifications()));
            writeLabelValue(nursingSheet, nursingRow, "Exécutions", String.valueOf(stats.nursing().executedPrescriptions()));

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Génération Excel impossible", ex);
        }
    }

    private void writeCountSheet(Sheet sheet, List<ActivityCountItem> items) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Libellé");
        header.createCell(1).setCellValue("Total");
        int rowIdx = 1;
        for (ActivityCountItem item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.key());
            row.createCell(1).setCellValue(item.count());
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private int writeLabelValue(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }
}

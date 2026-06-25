package com.afya.platform.nursing.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.nursing.dto.VitalSignAlertResponse;
import com.afya.platform.nursing.dto.VitalSignCreateRequest;
import com.afya.platform.nursing.dto.VitalSignResponse;
import com.afya.platform.nursing.integration.AdmissionLookup;
import com.afya.platform.nursing.integration.AdmissionServiceClient;
import com.afya.platform.nursing.model.VitalSignAlert;
import com.afya.platform.nursing.model.VitalSignReading;
import com.afya.platform.nursing.repository.VitalSignAlertRepository;
import com.afya.platform.nursing.repository.VitalSignReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VitalSignService {

    private final VitalSignReadingRepository vitalSignReadingRepository;
    private final VitalSignAlertRepository vitalSignAlertRepository;
    private final AdmissionServiceClient admissionServiceClient;
    private final AuditEventPublisher auditEventPublisher;

    public VitalSignService(
            VitalSignReadingRepository vitalSignReadingRepository,
            VitalSignAlertRepository vitalSignAlertRepository,
            AdmissionServiceClient admissionServiceClient,
            AuditEventPublisher auditEventPublisher
    ) {
        this.vitalSignReadingRepository = vitalSignReadingRepository;
        this.vitalSignAlertRepository = vitalSignAlertRepository;
        this.admissionServiceClient = admissionServiceClient;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<VitalSignResponse> listByAdmission(Long admissionId, String authorizationHeader) {
        admissionServiceClient.getAdmission(admissionId, authorizationHeader);
        List<VitalSignReading> readings =
                vitalSignReadingRepository.findByAdmissionIdOrderByRecordedAtDesc(admissionId);
        Map<Long, List<VitalSignAlertResponse>> alertsByReading = loadAlertsByReading(readings);
        return readings.stream()
                .map(reading -> toResponse(reading, alertsByReading.getOrDefault(reading.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VitalSignAlertResponse> listAlertsByAdmission(Long admissionId, String authorizationHeader) {
        admissionServiceClient.getAdmission(admissionId, authorizationHeader);
        return vitalSignAlertRepository.findByAdmissionId(admissionId).stream()
                .map(this::toAlertResponse)
                .toList();
    }

    @Transactional
    public VitalSignResponse create(
            Long admissionId,
            VitalSignCreateRequest request,
            String nurseUsername,
            String authorizationHeader
    ) {
        AdmissionLookup admission = admissionServiceClient.getAdmission(admissionId, authorizationHeader);

        VitalSignReading reading = new VitalSignReading();
        reading.setPatientId(admission.patientId());
        reading.setAdmissionId(admissionId);
        reading.setNurseUsername(nurseUsername);
        reading.setRecordedAt(request.recordedAt() != null ? request.recordedAt() : Instant.now());
        reading.setSlot(request.slot());
        reading.setSystolicBp(request.systolicBp());
        reading.setDiastolicBp(request.diastolicBp());
        reading.setPulseBpm(request.pulseBpm());
        reading.setRespiratoryRate(request.respiratoryRate());
        reading.setTemperatureCelsius(request.temperatureCelsius());
        reading.setWeightKg(request.weightKg());
        reading.setSpo2(request.spo2());
        reading.setDiuresisMl(request.diuresisMl());
        reading.setStoolsNote(blankToNull(request.stoolsNote()));

        VitalSignReading saved = vitalSignReadingRepository.save(reading);
        List<VitalSignAlertResponse> alerts = persistAlerts(saved);

        auditEventPublisher.publish(
                "VITAL_SIGN_RECORDED",
                "VITAL_SIGN",
                AuditMetadata.resourceId(saved.getId()),
                nurseUsername,
                AuditMetadata.patientId(saved.getPatientId()));

        if (!alerts.isEmpty()) {
            auditEventPublisher.publish(
                    "VITAL_SIGN_ALERT_RAISED",
                    "VITAL_SIGN_ALERT",
                    AuditMetadata.resourceId(saved.getId()),
                    nurseUsername,
                    AuditMetadata.patientId(saved.getPatientId()));
        }

        return toResponse(saved, alerts);
    }

    private List<VitalSignAlertResponse> persistAlerts(VitalSignReading reading) {
        List<VitalSignAlertResponse> responses = new ArrayList<>();
        for (VitalSignThresholdEvaluator.AlertDraft draft : VitalSignThresholdEvaluator.evaluate(reading)) {
            VitalSignAlert alert = new VitalSignAlert();
            alert.setVitalSignReading(reading);
            alert.setParameter(draft.parameter());
            alert.setMeasuredValue(draft.measuredValue());
            alert.setThresholdLabel(draft.thresholdLabel());
            alert.setAlertLevel(draft.alertLevel());
            VitalSignAlert saved = vitalSignAlertRepository.save(alert);
            responses.add(toAlertResponse(saved));
        }
        return responses;
    }

    private Map<Long, List<VitalSignAlertResponse>> loadAlertsByReading(List<VitalSignReading> readings) {
        if (readings.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = readings.stream().map(VitalSignReading::getId).toList();
        return vitalSignAlertRepository.findByReadingIds(ids).stream()
                .collect(Collectors.groupingBy(
                        alert -> alert.getVitalSignReading().getId(),
                        Collectors.mapping(this::toAlertResponse, Collectors.toList())));
    }

    private VitalSignResponse toResponse(VitalSignReading reading, List<VitalSignAlertResponse> alerts) {
        return new VitalSignResponse(
                reading.getId(),
                reading.getPatientId(),
                reading.getAdmissionId(),
                reading.getNurseUsername(),
                reading.getRecordedAt(),
                reading.getSlot(),
                reading.getSystolicBp(),
                reading.getDiastolicBp(),
                reading.getPulseBpm(),
                reading.getRespiratoryRate(),
                reading.getTemperatureCelsius(),
                reading.getWeightKg(),
                reading.getSpo2(),
                reading.getDiuresisMl(),
                reading.getStoolsNote(),
                alerts);
    }

    private VitalSignAlertResponse toAlertResponse(VitalSignAlert alert) {
        return new VitalSignAlertResponse(
                alert.getId(),
                alert.getParameter(),
                alert.getMeasuredValue(),
                alert.getThresholdLabel(),
                alert.getAlertLevel(),
                alert.getAlertAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}

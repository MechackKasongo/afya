package com.afya.platform.careentry.service;

import com.afya.platform.careentry.dto.VitalSignCreateRequest;
import com.afya.platform.careentry.dto.VitalSignResponse;
import com.afya.platform.careentry.model.VitalSignReading;
import com.afya.platform.careentry.repository.AdmissionRepository;
import com.afya.platform.careentry.repository.VitalSignReadingRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class VitalSignService {

    private final VitalSignReadingRepository vitalSignReadingRepository;
    private final AdmissionRepository admissionRepository;
    private final AuditEventPublisher auditEventPublisher;

    public VitalSignService(
            VitalSignReadingRepository vitalSignReadingRepository,
            AdmissionRepository admissionRepository,
            AuditEventPublisher auditEventPublisher
    ) {
        this.vitalSignReadingRepository = vitalSignReadingRepository;
        this.admissionRepository = admissionRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<VitalSignResponse> listByAdmission(Long admissionId) {
        ensureAdmissionExists(admissionId);
        return vitalSignReadingRepository.findByAdmissionIdOrderByRecordedAtDesc(admissionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VitalSignResponse create(Long admissionId, VitalSignCreateRequest request, String username) {
        var admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new NotFoundException("Admission introuvable : " + admissionId));

        VitalSignReading reading = new VitalSignReading();
        reading.setAdmissionId(admissionId);
        reading.setRecordedAt(request.recordedAt() != null ? request.recordedAt() : Instant.now());
        reading.setSlot(request.slot());
        reading.setSystolicBp(request.systolicBp());
        reading.setDiastolicBp(request.diastolicBp());
        reading.setPulseBpm(request.pulseBpm());
        reading.setTemperatureCelsius(request.temperatureCelsius());
        reading.setWeightKg(request.weightKg());
        reading.setDiuresisMl(request.diuresisMl());
        reading.setStoolsNote(blankToNull(request.stoolsNote()));

        VitalSignReading saved = vitalSignReadingRepository.save(reading);
        auditEventPublisher.publish(
                "VITAL_SIGN_RECORDED",
                "VITAL_SIGN",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.patientId(admission.getPatientId()));
        return toResponse(saved);
    }

    private void ensureAdmissionExists(Long admissionId) {
        if (!admissionRepository.existsById(admissionId)) {
            throw new NotFoundException("Admission introuvable : " + admissionId);
        }
    }

    private VitalSignResponse toResponse(VitalSignReading reading) {
        return new VitalSignResponse(
                reading.getId(),
                reading.getAdmissionId(),
                reading.getRecordedAt(),
                reading.getSlot(),
                reading.getSystolicBp(),
                reading.getDiastolicBp(),
                reading.getPulseBpm(),
                reading.getTemperatureCelsius(),
                reading.getWeightKg(),
                reading.getDiuresisMl(),
                reading.getStoolsNote()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}

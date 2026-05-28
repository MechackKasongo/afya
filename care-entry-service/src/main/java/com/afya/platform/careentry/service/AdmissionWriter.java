package com.afya.platform.careentry.service;

import com.afya.platform.careentry.model.Admission;
import com.afya.platform.careentry.repository.AdmissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists admissions in an independent transaction so downstream HTTP calls
 * (stay-service validating via GET /admissions/{id}) see committed rows.
 */
@Component
public class AdmissionWriter {

    private final AdmissionRepository admissionRepository;

    public AdmissionWriter(AdmissionRepository admissionRepository) {
        this.admissionRepository = admissionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Admission persist(Admission admission) {
        return admissionRepository.save(admission);
    }
}

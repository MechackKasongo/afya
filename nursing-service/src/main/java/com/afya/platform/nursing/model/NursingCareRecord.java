package com.afya.platform.nursing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "nursing_care_records")
public class NursingCareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medical_record_id", nullable = false)
    private Long medicalRecordId;

    /**
     * Identifiant de la ligne de prescription à l'origine de ce soin — MD-08.
     * Nullable : un soin infirmier peut être enregistré sans prescription associée
     * (ex. soin de confort, hygiène, surveillance).
     */
    @Column(name = "prescription_line_id")
    private Long prescriptionLineId;

    @Column(nullable = false, length = 80)
    private String careType;

    @Column(nullable = false)
    private Instant performedAt = Instant.now();

    @Column(nullable = false, length = 80)
    private String nurseUsername;

    @Column(nullable = false, length = 2000)
    private String description;

    public Long getId() {
        return id;
    }

    public Long getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(Long medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public Long getPrescriptionLineId() {
        return prescriptionLineId;
    }

    public void setPrescriptionLineId(Long prescriptionLineId) {
        this.prescriptionLineId = prescriptionLineId;
    }
    public String getCareType() {
        return careType;
    }

    public void setCareType(String careType) {
        this.careType = careType;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getNurseUsername() {
        return nurseUsername;
    }

    public void setNurseUsername(String nurseUsername) {
        this.nurseUsername = nurseUsername;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

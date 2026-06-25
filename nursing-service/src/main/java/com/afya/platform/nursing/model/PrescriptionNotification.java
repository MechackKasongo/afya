package com.afya.platform.nursing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "prescription_notifications")
public class PrescriptionNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_line_id", nullable = false, unique = true)
    private Long prescriptionLineId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "nurse_username", length = 80)
    private String nurseUsername;

    @Column(name = "medication_administration_id")
    private Long medicationAdministrationId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionNotificationStatus status = PrescriptionNotificationStatus.ENVOYEE;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    public Long getId() {
        return id;
    }

    public Long getPrescriptionLineId() {
        return prescriptionLineId;
    }

    public void setPrescriptionLineId(Long prescriptionLineId) {
        this.prescriptionLineId = prescriptionLineId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getNurseUsername() {
        return nurseUsername;
    }

    public void setNurseUsername(String nurseUsername) {
        this.nurseUsername = nurseUsername;
    }

    public Long getMedicationAdministrationId() {
        return medicationAdministrationId;
    }

    public void setMedicationAdministrationId(Long medicationAdministrationId) {
        this.medicationAdministrationId = medicationAdministrationId;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public PrescriptionNotificationStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionNotificationStatus status) {
        this.status = status;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}

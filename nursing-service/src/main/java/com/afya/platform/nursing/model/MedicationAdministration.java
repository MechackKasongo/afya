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
import java.time.LocalDate;

@Entity
@Table(name = "medication_administrations")
public class MedicationAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_line_id", nullable = false)
    private Long prescriptionLineId;

    @Column(name = "medical_record_id", nullable = false)
    private Long medicalRecordId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "administration_date", nullable = false)
    private LocalDate administrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VitalSignSlot slot;

    @Column(nullable = false)
    private boolean administered = true;

    @Column(nullable = false)
    private Instant administeredAt = Instant.now();

    @Column(length = 80)
    private String doseGiven;

    @Column(nullable = false, length = 80)
    private String nurseUsername;

    @Column(length = 500)
    private String notes;

    public Long getId() {
        return id;
    }

    public Long getPrescriptionLineId() {
        return prescriptionLineId;
    }

    public void setPrescriptionLineId(Long prescriptionLineId) {
        this.prescriptionLineId = prescriptionLineId;
    }

    public Long getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(Long medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public LocalDate getAdministrationDate() {
        return administrationDate;
    }

    public void setAdministrationDate(LocalDate administrationDate) {
        this.administrationDate = administrationDate;
    }

    public VitalSignSlot getSlot() {
        return slot;
    }

    public void setSlot(VitalSignSlot slot) {
        this.slot = slot;
    }

    public boolean isAdministered() {
        return administered;
    }

    public void setAdministered(boolean administered) {
        this.administered = administered;
    }

    public Instant getAdministeredAt() {
        return administeredAt;
    }

    public String getDoseGiven() {
        return doseGiven;
    }

    public void setDoseGiven(String doseGiven) {
        this.doseGiven = doseGiven;
    }

    public String getNurseUsername() {
        return nurseUsername;
    }

    public void setNurseUsername(String nurseUsername) {
        this.nurseUsername = nurseUsername;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package com.afya.platform.admission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "admissions")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long hospitalServiceId;

    @Column(nullable = false)
    private Instant admittedAt;

    private Instant dischargedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionStatus status;

    @Column(length = 255)
    private String dischargeReason;

    @Column(name = "admission_reason", length = 255)
    private String admissionReason;

    /**
     * Numéro d'admission lisible — MD-05 (numAdmission du mémoire).
     * Généré sous la forme {@code ADM-AAAA-NNNNN} par {@code AdmissionService}.
     */
    @Column(name = "admission_number", unique = true, length = 20)
    private String admissionNumber;

    /**
     * Type d'admission — MD-05.
     * {@link AdmissionType#NORMALE} ou {@link AdmissionType#URGENCE}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "admission_type", nullable = false, length = 10)
    private AdmissionType admissionType = AdmissionType.NORMALE;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getHospitalServiceId() {
        return hospitalServiceId;
    }

    public void setHospitalServiceId(Long hospitalServiceId) {
        this.hospitalServiceId = hospitalServiceId;
    }

    public Instant getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(Instant admittedAt) {
        this.admittedAt = admittedAt;
    }

    public Instant getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(Instant dischargedAt) {
        this.dischargedAt = dischargedAt;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public void setStatus(AdmissionStatus status) {
        this.status = status;
    }

    public String getDischargeReason() {
        return dischargeReason;
    }

    public void setDischargeReason(String dischargeReason) {
        this.dischargeReason = dischargeReason;
    }

    public String getAdmissionReason() {
        return admissionReason;
    }

    public void setAdmissionReason(String admissionReason) {
        this.admissionReason = admissionReason;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

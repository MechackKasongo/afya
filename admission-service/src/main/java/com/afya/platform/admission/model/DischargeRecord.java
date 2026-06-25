package com.afya.platform.admission.model;

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
@Table(name = "discharges")
public class DischargeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_id", nullable = false, unique = true)
    private Long admissionId;

    @Column(name = "discharged_at", nullable = false)
    private Instant dischargedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "discharge_type", nullable = false, length = 30)
    private DischargeType dischargeType;

    @Column(name = "post_discharge_instructions", length = 2000)
    private String postDischargeInstructions;

    @Column(name = "recorded_by_username", nullable = false, length = 80)
    private String recordedByUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Instant getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(Instant dischargedAt) {
        this.dischargedAt = dischargedAt;
    }

    public DischargeType getDischargeType() {
        return dischargeType;
    }

    public void setDischargeType(DischargeType dischargeType) {
        this.dischargeType = dischargeType;
    }

    public String getPostDischargeInstructions() {
        return postDischargeInstructions;
    }

    public void setPostDischargeInstructions(String postDischargeInstructions) {
        this.postDischargeInstructions = postDischargeInstructions;
    }

    public String getRecordedByUsername() {
        return recordedByUsername;
    }

    public void setRecordedByUsername(String recordedByUsername) {
        this.recordedByUsername = recordedByUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

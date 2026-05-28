package com.afya.platform.careentry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "transfer_requests")
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @Column(nullable = false)
    private Long fromServiceId;

    @Column(nullable = false)
    private Long toServiceId;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(length = 255)
    private String reason;

    public Long getId() {
        return id;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    public Long getFromServiceId() {
        return fromServiceId;
    }

    public void setFromServiceId(Long fromServiceId) {
        this.fromServiceId = fromServiceId;
    }

    public Long getToServiceId() {
        return toServiceId;
    }

    public void setToServiceId(Long toServiceId) {
        this.toServiceId = toServiceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

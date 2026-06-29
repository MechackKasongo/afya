package com.afya.platform.lab.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_requests")
public class ExamRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExamUrgency urgency = ExamUrgency.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamRequestStatus status = ExamRequestStatus.PENDING;

    @Column(length = 1000)
    private String comment;

    @Column(name = "postpone_reason", length = 1000)
    private String postponeReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamRequestLine> lines = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public ExamUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(ExamUrgency urgency) {
        this.urgency = urgency;
    }

    public ExamRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ExamRequestStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPostponeReason() {
        return postponeReason;
    }

    public void setPostponeReason(String postponeReason) {
        this.postponeReason = postponeReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExamRequestLine> getLines() {
        return lines;
    }

    public void addLine(ExamRequestLine line) {
        lines.add(line);
        line.setRequest(this);
    }
}

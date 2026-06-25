package com.afya.platform.nursing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vital_sign_alerts")
public class VitalSignAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vital_sign_reading_id", nullable = false)
    private VitalSignReading vitalSignReading;

    @Column(nullable = false, length = 40)
    private String parameter;

    @Column(name = "measured_value", nullable = false, length = 40)
    private String measuredValue;

    @Column(name = "threshold_label", nullable = false, length = 80)
    private String thresholdLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false, length = 20)
    private VitalSignAlertLevel alertLevel;

    @Column(name = "alert_at", nullable = false)
    private Instant alertAt = Instant.now();

    public Long getId() {
        return id;
    }

    public VitalSignReading getVitalSignReading() {
        return vitalSignReading;
    }

    public void setVitalSignReading(VitalSignReading vitalSignReading) {
        this.vitalSignReading = vitalSignReading;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(String measuredValue) {
        this.measuredValue = measuredValue;
    }

    public String getThresholdLabel() {
        return thresholdLabel;
    }

    public void setThresholdLabel(String thresholdLabel) {
        this.thresholdLabel = thresholdLabel;
    }

    public VitalSignAlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(VitalSignAlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public Instant getAlertAt() {
        return alertAt;
    }
}

package com.afya.platform.lab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "result_parameters")
public class ResultParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private ExamResult result;

    @Column(name = "parameter_name", nullable = false, length = 120)
    private String parameterName;

    @Column(nullable = false, length = 120)
    private String value;

    @Column(length = 40)
    private String unit;

    @Column(name = "reference_min", length = 40)
    private String referenceMin;

    @Column(name = "reference_max", length = 40)
    private String referenceMax;

    @Column(nullable = false)
    private boolean abnormal;

    public Long getId() {
        return id;
    }

    public ExamResult getResult() {
        return result;
    }

    public void setResult(ExamResult result) {
        this.result = result;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getReferenceMin() {
        return referenceMin;
    }

    public void setReferenceMin(String referenceMin) {
        this.referenceMin = referenceMin;
    }

    public String getReferenceMax() {
        return referenceMax;
    }

    public void setReferenceMax(String referenceMax) {
        this.referenceMax = referenceMax;
    }

    public boolean isAbnormal() {
        return abnormal;
    }

    public void setAbnormal(boolean abnormal) {
        this.abnormal = abnormal;
    }
}

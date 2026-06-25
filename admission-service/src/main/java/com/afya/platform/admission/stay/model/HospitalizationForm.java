package com.afya.platform.admission.stay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "hospitalization_forms")
public class HospitalizationForm {

    @Id
    private Long stayId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "stay_id")
    private Stay stay;

    @Column(name = "antecedents_text", columnDefinition = "TEXT")
    private String antecedentsText;

    @Column(name = "anamnesis_text", columnDefinition = "TEXT")
    private String anamnesisText;

    @Column(name = "physical_exam_pulmonary_text", columnDefinition = "TEXT")
    private String physicalExamPulmonaryText;

    @Column(name = "physical_exam_cardiac_text", columnDefinition = "TEXT")
    private String physicalExamCardiacText;

    @Column(name = "physical_exam_abdominal_text", columnDefinition = "TEXT")
    private String physicalExamAbdominalText;

    @Column(name = "physical_exam_neurological_text", columnDefinition = "TEXT")
    private String physicalExamNeurologicalText;

    @Column(name = "physical_exam_misc_text", columnDefinition = "TEXT")
    private String physicalExamMiscText;

    @Column(name = "paraclinical_text", columnDefinition = "TEXT")
    private String paraclinicalText;

    @Column(name = "conclusion_text", columnDefinition = "TEXT")
    private String conclusionText;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getStayId() {
        return stayId;
    }

    public Stay getStay() {
        return stay;
    }

    public void setStay(Stay stay) {
        this.stay = stay;
    }

    public String getAntecedentsText() {
        return antecedentsText;
    }

    public void setAntecedentsText(String antecedentsText) {
        this.antecedentsText = antecedentsText;
    }

    public String getAnamnesisText() {
        return anamnesisText;
    }

    public void setAnamnesisText(String anamnesisText) {
        this.anamnesisText = anamnesisText;
    }

    public String getPhysicalExamPulmonaryText() {
        return physicalExamPulmonaryText;
    }

    public void setPhysicalExamPulmonaryText(String physicalExamPulmonaryText) {
        this.physicalExamPulmonaryText = physicalExamPulmonaryText;
    }

    public String getPhysicalExamCardiacText() {
        return physicalExamCardiacText;
    }

    public void setPhysicalExamCardiacText(String physicalExamCardiacText) {
        this.physicalExamCardiacText = physicalExamCardiacText;
    }

    public String getPhysicalExamAbdominalText() {
        return physicalExamAbdominalText;
    }

    public void setPhysicalExamAbdominalText(String physicalExamAbdominalText) {
        this.physicalExamAbdominalText = physicalExamAbdominalText;
    }

    public String getPhysicalExamNeurologicalText() {
        return physicalExamNeurologicalText;
    }

    public void setPhysicalExamNeurologicalText(String physicalExamNeurologicalText) {
        this.physicalExamNeurologicalText = physicalExamNeurologicalText;
    }

    public String getPhysicalExamMiscText() {
        return physicalExamMiscText;
    }

    public void setPhysicalExamMiscText(String physicalExamMiscText) {
        this.physicalExamMiscText = physicalExamMiscText;
    }

    public String getParaclinicalText() {
        return paraclinicalText;
    }

    public void setParaclinicalText(String paraclinicalText) {
        this.paraclinicalText = paraclinicalText;
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

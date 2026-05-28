package com.afya.platform.clinical.model;

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
@Table(name = "medication_administrations")
public class MedicationAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_line_id", nullable = false)
    private PrescriptionLine prescriptionLine;

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

    public PrescriptionLine getPrescriptionLine() {
        return prescriptionLine;
    }

    public void setPrescriptionLine(PrescriptionLine prescriptionLine) {
        this.prescriptionLine = prescriptionLine;
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

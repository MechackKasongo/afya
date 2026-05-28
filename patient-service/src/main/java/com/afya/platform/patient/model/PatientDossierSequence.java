package com.afya.platform.patient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient_dossier_sequences")
public class PatientDossierSequence {

    @Id
    @Column(name = "sequence_year")
    private int sequenceYear;

    @Column(nullable = false, length = 4)
    private String letterBlock = "AAAA";

    @Column(nullable = false)
    private int sequenceNumber;

    public int getSequenceYear() {
        return sequenceYear;
    }

    public void setSequenceYear(int sequenceYear) {
        this.sequenceYear = sequenceYear;
    }

    public String getLetterBlock() {
        return letterBlock;
    }

    public void setLetterBlock(String letterBlock) {
        this.letterBlock = letterBlock;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}

package com.afya.platform.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "disease_catalog")
public class DiseaseCatalog {

    public static final int MIN_USAGE_FOR_SELECTION = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disease_type", nullable = false, length = 120)
    private String diseaseType;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(name = "label_normalized", nullable = false, length = 255)
    private String labelNormalized;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "first_used_at", nullable = false)
    private Instant firstUsedAt = Instant.now();

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getDiseaseType() {
        return diseaseType;
    }

    public void setDiseaseType(String diseaseType) {
        this.diseaseType = diseaseType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelNormalized() {
        return labelNormalized;
    }

    public void setLabelNormalized(String labelNormalized) {
        this.labelNormalized = labelNormalized;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public Instant getFirstUsedAt() {
        return firstUsedAt;
    }

    public void setFirstUsedAt(Instant firstUsedAt) {
        this.firstUsedAt = firstUsedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public boolean isSelectable() {
        return usageCount >= MIN_USAGE_FOR_SELECTION;
    }
}

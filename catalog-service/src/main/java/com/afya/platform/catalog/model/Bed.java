package com.afya.platform.catalog.model;

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
@Table(name = "beds")
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_service_id", nullable = false)
    private HospitalService hospitalService;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(nullable = false)
    private boolean occupied;

    @Column(name = "last_freed_at")
    private Instant lastFreedAt;

    public Long getId() {
        return id;
    }

    public HospitalService getHospitalService() {
        return hospitalService;
    }

    public void setHospitalService(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public Instant getLastFreedAt() {
        return lastFreedAt;
    }

    public void setLastFreedAt(Instant lastFreedAt) {
        this.lastFreedAt = lastFreedAt;
    }
}

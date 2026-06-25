package com.afya.platform.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "hospital_services")
public class HospitalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false)
    private int bedCapacity;

    /** Nombre de lits numérotés dans chaque chambre (01, 02…). La capacité totale = nombre de lits. */
    @Column(name = "beds_per_room", nullable = false)
    private int bedsPerRoom = 1;

    /** Lettre devant le numéro de chambre (ex. A → A1, A2). */
    @Column(name = "room_letter_prefix", nullable = false, length = 1)
    private char roomLetterPrefix = 'A';

    @Enumerated(EnumType.STRING)
    @Column(name = "bed_assignment_policy", nullable = false, length = 32)
    private BedAssignmentPolicy bedAssignmentPolicy = BedAssignmentPolicy.ROOM_ORDER_ASC;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBedCapacity() {
        return bedCapacity;
    }

    public void setBedCapacity(int bedCapacity) {
        this.bedCapacity = bedCapacity;
    }

    public int getBedsPerRoom() {
        return bedsPerRoom;
    }

    public void setBedsPerRoom(int bedsPerRoom) {
        this.bedsPerRoom = bedsPerRoom;
    }

    public char getRoomLetterPrefix() {
        return roomLetterPrefix;
    }

    public void setRoomLetterPrefix(char roomLetterPrefix) {
        this.roomLetterPrefix = roomLetterPrefix;
    }

    public BedAssignmentPolicy getBedAssignmentPolicy() {
        return bedAssignmentPolicy;
    }

    public void setBedAssignmentPolicy(BedAssignmentPolicy bedAssignmentPolicy) {
        this.bedAssignmentPolicy = bedAssignmentPolicy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

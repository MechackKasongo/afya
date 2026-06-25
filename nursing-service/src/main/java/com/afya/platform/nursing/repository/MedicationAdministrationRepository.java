package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    boolean existsByPrescriptionLineId(Long prescriptionLineId);

    @Query("""
            SELECT m.prescriptionLineId FROM MedicationAdministration m
            WHERE m.medicalRecordId = :medicalRecordId
            """)
    List<Long> findAdministeredLineIdsByMedicalRecordId(@Param("medicalRecordId") Long medicalRecordId);
}

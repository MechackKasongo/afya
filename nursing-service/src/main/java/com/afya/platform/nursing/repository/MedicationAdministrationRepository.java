package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    boolean existsByPrescriptionLineId(Long prescriptionLineId);

    Optional<MedicationAdministration> findByPrescriptionLineIdAndAdministrationDateAndSlot(
            Long prescriptionLineId,
            LocalDate administrationDate,
            com.afya.platform.nursing.model.VitalSignSlot slot
    );

    List<MedicationAdministration> findByPrescriptionLineIdOrderByAdministrationDateDescSlotAsc(Long prescriptionLineId);

    @Query("""
            SELECT DISTINCT m.prescriptionLineId FROM MedicationAdministration m
            WHERE m.medicalRecordId = :medicalRecordId
              AND m.administered = true
            """)
    List<Long> findAdministeredLineIdsByMedicalRecordId(@Param("medicalRecordId") Long medicalRecordId);
}

package com.afya.platform.clinical.repository;

import com.afya.platform.clinical.model.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    List<MedicationAdministration> findByPrescriptionLineIdOrderByAdministeredAtDesc(Long prescriptionLineId);

    boolean existsByPrescriptionLineId(Long prescriptionLineId);
}

package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    @Query("""
            SELECT c FROM Consultation c
            WHERE (:patientId IS NULL OR c.patientId = :patientId)
              AND (:admissionId IS NULL OR c.admissionId = :admissionId)
            """)
    Page<Consultation> search(
            @Param("patientId") Long patientId,
            @Param("admissionId") Long admissionId,
            Pageable pageable
    );
}

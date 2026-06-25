package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.VitalSignAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VitalSignAlertRepository extends JpaRepository<VitalSignAlert, Long> {

    @Query("""
            SELECT a FROM VitalSignAlert a
            JOIN FETCH a.vitalSignReading r
            WHERE r.id IN :readingIds
            ORDER BY a.alertAt DESC
            """)
    List<VitalSignAlert> findByReadingIds(@Param("readingIds") List<Long> readingIds);

    @Query("""
            SELECT a FROM VitalSignAlert a
            JOIN a.vitalSignReading r
            WHERE r.admissionId = :admissionId
            ORDER BY a.alertAt DESC
            """)
    List<VitalSignAlert> findByAdmissionId(@Param("admissionId") Long admissionId);
}

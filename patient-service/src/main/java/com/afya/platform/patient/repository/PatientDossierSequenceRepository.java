package com.afya.platform.patient.repository;

import com.afya.platform.patient.model.PatientDossierSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientDossierSequenceRepository extends JpaRepository<PatientDossierSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PatientDossierSequence s WHERE s.sequenceYear = :year")
    Optional<PatientDossierSequence> findByYearForUpdate(@Param("year") int year);
}

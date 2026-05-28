package com.afya.platform.patient.repository;

import com.afya.platform.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByDossierNumber(String dossierNumber);

    Optional<Patient> findByDossierNumber(String dossierNumber);

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDateAndSex(
            String firstName,
            String lastName,
            LocalDate birthDate,
            String sex
    );

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDateAndSexAndIdNot(
            String firstName,
            String lastName,
            LocalDate birthDate,
            String sex,
            Long id
    );

    @Query("""
            SELECT p FROM Patient p
            WHERE :query IS NULL OR :query = '' OR
                  LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR
                  LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR
                  LOWER(p.dossierNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
                  LOWER(COALESCE(p.postName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Patient> search(@Param("query") String query, Pageable pageable);
}

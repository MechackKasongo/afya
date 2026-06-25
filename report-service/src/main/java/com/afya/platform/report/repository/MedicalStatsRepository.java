package com.afya.platform.report.repository;

import com.afya.platform.report.model.MedicalStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalStatsRepository extends JpaRepository<MedicalStats, Long> {

    Optional<MedicalStats> findByStatDate(LocalDate statDate);

    List<MedicalStats> findByStatDateBetweenOrderByStatDateDesc(LocalDate from, LocalDate to);

    Page<MedicalStats> findAllByOrderByStatDateDesc(Pageable pageable);
}

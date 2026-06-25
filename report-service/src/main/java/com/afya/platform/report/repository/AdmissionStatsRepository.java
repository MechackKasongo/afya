package com.afya.platform.report.repository;

import com.afya.platform.report.model.AdmissionStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionStatsRepository extends JpaRepository<AdmissionStats, Long> {

    Optional<AdmissionStats> findByStatDate(LocalDate statDate);

    List<AdmissionStats> findByStatDateBetweenOrderByStatDateDesc(LocalDate from, LocalDate to);

    Page<AdmissionStats> findAllByOrderByStatDateDesc(Pageable pageable);
}

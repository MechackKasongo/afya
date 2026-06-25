package com.afya.platform.report.repository;

import com.afya.platform.report.model.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {
}

package com.afya.platform.lab.repository;

import com.afya.platform.lab.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    @Query("SELECT r FROM ExamResult r WHERE r.request.id = :requestId")
    Optional<ExamResult> findByRequestId(@Param("requestId") Long requestId);

    @Query("""
            SELECT COUNT(p) FROM ResultParameter p
            JOIN p.result r
            WHERE p.abnormal = true AND r.resultedAt >= :from AND r.resultedAt < :to
            """)
    long countAbnormalParametersBetween(@Param("from") Instant from, @Param("to") Instant to);
}

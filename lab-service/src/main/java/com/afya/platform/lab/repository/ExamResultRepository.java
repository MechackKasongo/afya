package com.afya.platform.lab.repository;

import com.afya.platform.lab.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    @Query("SELECT r FROM ExamResult r WHERE r.request.id = :requestId")
    Optional<ExamResult> findByRequestId(@Param("requestId") Long requestId);
}

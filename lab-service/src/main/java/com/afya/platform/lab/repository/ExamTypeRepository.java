package com.afya.platform.lab.repository;

import com.afya.platform.lab.model.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamTypeRepository extends JpaRepository<ExamType, Long> {

    List<ExamType> findByActiveTrueOrderByNameAsc();

    Optional<ExamType> findByNameIgnoreCase(String name);
}

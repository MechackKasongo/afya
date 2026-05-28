package com.afya.platform.catalog.repository;

import com.afya.platform.catalog.model.HospitalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

public interface HospitalServiceRepository extends JpaRepository<HospitalService, Long> {

    @EntityGraph(attributePaths = "department")
    List<HospitalService> findByActiveTrueOrderByNameAsc();

    Optional<HospitalService> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Page<HospitalService> findByActive(boolean active, Pageable pageable);
}

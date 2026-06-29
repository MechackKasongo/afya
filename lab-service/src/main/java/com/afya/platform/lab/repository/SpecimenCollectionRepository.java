package com.afya.platform.lab.repository;

import com.afya.platform.lab.model.SpecimenCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecimenCollectionRepository extends JpaRepository<SpecimenCollection, Long> {

    Optional<SpecimenCollection> findByRequestId(Long requestId);
}

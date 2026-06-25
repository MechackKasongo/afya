package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.DiseaseCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiseaseCatalogRepository extends JpaRepository<DiseaseCatalog, Long> {

    Optional<DiseaseCatalog> findByDiseaseTypeAndLabelNormalized(String diseaseType, String labelNormalized);

    List<DiseaseCatalog> findByDiseaseTypeAndUsageCountGreaterThanEqualOrderByUsageCountDescLabelAsc(
            String diseaseType,
            int minUsage
    );
}

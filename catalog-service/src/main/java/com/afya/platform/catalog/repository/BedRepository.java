package com.afya.platform.catalog.repository;

import com.afya.platform.catalog.model.Bed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByHospitalServiceIdOrderByLabelAsc(Long hospitalServiceId);

    List<Bed> findByHospitalServiceIdAndOccupiedFalseOrderByLabelAsc(Long hospitalServiceId);

    List<Bed> findByHospitalServiceIdAndOccupiedFalse(Long hospitalServiceId);

    Optional<Bed> findFirstByHospitalServiceIdAndOccupiedFalseOrderByLabelAsc(Long hospitalServiceId);

    Optional<Bed> findByHospitalServiceIdAndLabelIgnoreCase(Long hospitalServiceId, String label);

    long countByHospitalServiceId(Long hospitalServiceId);

    long countByHospitalServiceIdAndOccupiedTrue(Long hospitalServiceId);

    long countByOccupiedTrue();
}

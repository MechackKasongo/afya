package com.afya.platform.stay.repository;

import com.afya.platform.stay.model.HospitalizationForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalizationFormRepository extends JpaRepository<HospitalizationForm, Long> {
}

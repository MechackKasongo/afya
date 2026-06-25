package com.afya.platform.admission.stay.repository;

import com.afya.platform.admission.stay.model.HospitalizationForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalizationFormRepository extends JpaRepository<HospitalizationForm, Long> {
}

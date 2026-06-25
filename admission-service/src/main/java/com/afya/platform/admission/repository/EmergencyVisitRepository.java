package com.afya.platform.admission.repository;

import com.afya.platform.admission.model.EmergencyStatus;
import com.afya.platform.admission.model.EmergencyVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface EmergencyVisitRepository extends JpaRepository<EmergencyVisit, Long>, JpaSpecificationExecutor<EmergencyVisit> {

    long countByStatusIn(Collection<EmergencyStatus> statuses);
}

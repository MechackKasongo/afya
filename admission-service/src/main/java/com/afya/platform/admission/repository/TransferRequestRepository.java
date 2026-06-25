package com.afya.platform.admission.repository;

import com.afya.platform.admission.model.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    List<TransferRequest> findByAdmissionIdOrderByRequestedAtDesc(Long admissionId);
}

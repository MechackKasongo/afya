package com.afya.platform.careentry.repository;

import com.afya.platform.careentry.model.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    List<TransferRequest> findByAdmissionIdOrderByRequestedAtDesc(Long admissionId);
}

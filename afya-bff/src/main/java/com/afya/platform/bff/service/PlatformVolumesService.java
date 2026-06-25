package com.afya.platform.bff.service;

import com.afya.platform.bff.client.AdmissionClient;
import com.afya.platform.bff.client.AdmissionStayClient;
import com.afya.platform.bff.client.MedicalClient;
import com.afya.platform.bff.client.PatientClient;
import com.afya.platform.bff.dto.PlatformVolumesValue;
import org.springframework.stereotype.Service;

@Service
public class PlatformVolumesService {

    private final PatientClient patientClient;
    private final AdmissionClient admissionClient;
    private final AdmissionStayClient admissionStayClient;
    private final MedicalClient medicalClient;

    public PlatformVolumesService(
            PatientClient patientClient,
            AdmissionClient admissionClient,
            AdmissionStayClient admissionStayClient,
            MedicalClient medicalClient
    ) {
        this.patientClient = patientClient;
        this.admissionClient = admissionClient;
        this.admissionStayClient = admissionStayClient;
        this.medicalClient = medicalClient;
    }

    public PlatformVolumesValue aggregate(String authorizationHeader) {
        var patients = patientClient.volumes(authorizationHeader);
        var admissions = admissionClient.volumes(authorizationHeader);
        var stays = admissionStayClient.volumes(authorizationHeader);
        var clinical = medicalClient.volumes(authorizationHeader);
        return new PlatformVolumesValue(
                patients.patients(),
                admissions.admissions(),
                admissions.activeAdmissions(),
                admissions.transferredAdmissions(),
                admissions.dischargedAdmissions(),
                admissions.deceasedAdmissions(),
                admissions.emergencyVisits(),
                admissions.activeEmergencyVisits(),
                stays.openStays(),
                clinical.consultations(),
                clinical.clinicalDocuments());
    }
}

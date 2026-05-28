package com.afya.platform.bff.service;

import com.afya.platform.bff.client.CareEntryClient;
import com.afya.platform.bff.client.ClinicalRecordClient;
import com.afya.platform.bff.client.PatientClient;
import com.afya.platform.bff.client.StayClient;
import com.afya.platform.bff.dto.PlatformVolumesValue;
import org.springframework.stereotype.Service;

@Service
public class PlatformVolumesService {

    private final PatientClient patientClient;
    private final CareEntryClient careEntryClient;
    private final StayClient stayClient;
    private final ClinicalRecordClient clinicalRecordClient;

    public PlatformVolumesService(
            PatientClient patientClient,
            CareEntryClient careEntryClient,
            StayClient stayClient,
            ClinicalRecordClient clinicalRecordClient
    ) {
        this.patientClient = patientClient;
        this.careEntryClient = careEntryClient;
        this.stayClient = stayClient;
        this.clinicalRecordClient = clinicalRecordClient;
    }

    public PlatformVolumesValue aggregate(String authorizationHeader) {
        var patients = patientClient.volumes(authorizationHeader);
        var careEntry = careEntryClient.volumes(authorizationHeader);
        var stays = stayClient.volumes(authorizationHeader);
        var clinical = clinicalRecordClient.volumes(authorizationHeader);
        return new PlatformVolumesValue(
                patients.patients(),
                careEntry.admissions(),
                careEntry.activeAdmissions(),
                careEntry.transferredAdmissions(),
                careEntry.dischargedAdmissions(),
                careEntry.deceasedAdmissions(),
                careEntry.emergencyVisits(),
                careEntry.activeEmergencyVisits(),
                stays.openStays(),
                clinical.consultations(),
                clinical.clinicalDocuments());
    }
}

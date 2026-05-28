package com.afya.platform.bff.dto;

public record PlatformVolumesValue(
        long patients,
        long admissions,
        long activeAdmissions,
        long transferredAdmissions,
        long dischargedAdmissions,
        long deceasedAdmissions,
        long emergencyVisits,
        long activeEmergencyVisits,
        long openStays,
        long consultations,
        long clinicalDocuments
) {
}

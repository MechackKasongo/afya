package com.afya.platform.admission.dto;

public record PlatformVolumesResponse(
        long admissions,
        long activeAdmissions,
        long transferredAdmissions,
        long dischargedAdmissions,
        long deceasedAdmissions,
        long emergencyVisits,
        long activeEmergencyVisits
) {
}

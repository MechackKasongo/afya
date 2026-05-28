package com.afya.platform.bff.dto;

public record CareEntryVolumesResponse(
        long admissions,
        long activeAdmissions,
        long transferredAdmissions,
        long dischargedAdmissions,
        long deceasedAdmissions,
        long emergencyVisits,
        long activeEmergencyVisits
) {
}

package com.afya.platform.catalog.dto;

public record HospitalServiceResponse(
        Long id,
        Long departmentId,
        String departmentCode,
        String departmentName,
        String name,
        int bedCapacity,
        int bedsPerRoom,
        String roomLetterPrefix,
        int roomCount,
        long bedCount,
        String bedAssignmentPolicy,
        boolean active
) {
}

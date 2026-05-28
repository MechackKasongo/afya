package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HospitalServiceRequest(
        @NotNull Long departmentId,
        @NotBlank @Size(max = 120) String name,
        @Min(0) int bedCapacity,
        @Min(1) Integer bedsPerRoom,
        @Size(min = 1, max = 1) String roomLetterPrefix,
        /** ROOM_ORDER_ASC ou LONGEST_IDLE */
        String bedAssignmentPolicy
) {
}

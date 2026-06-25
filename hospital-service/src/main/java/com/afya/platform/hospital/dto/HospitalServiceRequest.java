package com.afya.platform.hospital.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HospitalServiceRequest(
        @NotNull(message = "Le département est obligatoire")
        Long departmentId,
        @NotBlank(message = "Le nom du service est obligatoire")
        @Size(max = 120)
        String name,
        @Min(value = 0, message = "La capacité doit être positive ou nulle")
        int bedCapacity,
        @Min(value = 1, message = "Au moins un lit par chambre")
        Integer bedsPerRoom,
        /** Lettre des chambres (ex. A → A1, A2) */
        @Size(min = 1, max = 1)
        String roomLetterPrefix,
        /** ROOM_ORDER_ASC (défaut) ou LONGEST_IDLE */
        String bedAssignmentPolicy,
        Boolean active
) {
}

package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Size;

public record DischargeLegacyRequest(@Size(max = 255) String note) {
}

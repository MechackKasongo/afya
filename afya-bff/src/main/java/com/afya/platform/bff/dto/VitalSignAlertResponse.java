package com.afya.platform.bff.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VitalSignAlertResponse(
        Long id,
        String parameter,
        String measuredValue,
        String thresholdLabel,
        String alertLevel,
        Instant alertAt
) {
}

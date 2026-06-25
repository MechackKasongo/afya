package com.afya.platform.nursing.dto;

import com.afya.platform.nursing.model.VitalSignAlertLevel;
import com.afya.platform.nursing.model.VitalSignSlot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VitalSignAlertResponse(
        Long id,
        String parameter,
        String measuredValue,
        String thresholdLabel,
        VitalSignAlertLevel alertLevel,
        Instant alertAt
) {
}

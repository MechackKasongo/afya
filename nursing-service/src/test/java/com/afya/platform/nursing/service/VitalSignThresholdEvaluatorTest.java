package com.afya.platform.nursing.service;

import com.afya.platform.nursing.model.VitalSignAlertLevel;
import com.afya.platform.nursing.model.VitalSignReading;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VitalSignThresholdEvaluatorTest {

    @Test
    void detectsCriticalTemperatureAndSpo2() {
        VitalSignReading reading = new VitalSignReading();
        reading.setTemperatureCelsius(new BigDecimal("39.5"));
        reading.setSpo2(new BigDecimal("88"));

        List<VitalSignThresholdEvaluator.AlertDraft> alerts = VitalSignThresholdEvaluator.evaluate(reading);

        assertThat(alerts).hasSize(2);
        assertThat(alerts).anyMatch(a ->
                "temperature".equals(a.parameter()) && a.alertLevel() == VitalSignAlertLevel.CRITIQUE);
        assertThat(alerts).anyMatch(a ->
                "spO2".equals(a.parameter()) && a.alertLevel() == VitalSignAlertLevel.CRITIQUE);
    }

    @Test
    void detectsAttentionBloodPressure() {
        VitalSignReading reading = new VitalSignReading();
        reading.setSystolicBp(145);
        reading.setDiastolicBp(92);

        List<VitalSignThresholdEvaluator.AlertDraft> alerts = VitalSignThresholdEvaluator.evaluate(reading);

        assertThat(alerts).hasSize(2);
        assertThat(alerts).allMatch(a -> a.alertLevel() == VitalSignAlertLevel.ATTENTION);
    }
}

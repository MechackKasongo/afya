package com.afya.platform.nursing.service;

import com.afya.platform.nursing.model.VitalSignAlertLevel;
import com.afya.platform.nursing.model.VitalSignReading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class VitalSignThresholdEvaluator {

    record AlertDraft(
            String parameter,
            String measuredValue,
            String thresholdLabel,
            VitalSignAlertLevel alertLevel
    ) {
    }

    private VitalSignThresholdEvaluator() {
    }

    static List<AlertDraft> evaluate(VitalSignReading reading) {
        List<AlertDraft> alerts = new ArrayList<>();
        evaluateTemperature(reading.getTemperatureCelsius(), alerts);
        evaluateSystolic(reading.getSystolicBp(), alerts);
        evaluateDiastolic(reading.getDiastolicBp(), alerts);
        evaluatePulse(reading.getPulseBpm(), alerts);
        evaluateSpo2(reading.getSpo2(), alerts);
        evaluateRespiratoryRate(reading.getRespiratoryRate(), alerts);
        alerts.sort(Comparator.comparing(AlertDraft::alertLevel).reversed());
        return alerts;
    }

    private static void evaluateTemperature(BigDecimal value, List<AlertDraft> alerts) {
        if (value == null) {
            return;
        }
        double temp = value.doubleValue();
        if (temp < 35 || temp > 39) {
            alerts.add(critical("temperature", value + " °C", "< 35 ou > 39 °C"));
        } else if (temp < 36 || temp > 37.5) {
            alerts.add(attention("temperature", value + " °C", "< 36 ou > 37,5 °C"));
        }
    }

    private static void evaluateSystolic(Integer value, List<AlertDraft> alerts) {
        if (value == null) {
            return;
        }
        if (value < 80 || value > 180) {
            alerts.add(critical("tensionSystolique", value + " mmHg", "< 80 ou > 180 mmHg"));
        } else if (value < 90 || value > 140) {
            alerts.add(attention("tensionSystolique", value + " mmHg", "< 90 ou > 140 mmHg"));
        }
    }

    private static void evaluateDiastolic(Integer value, List<AlertDraft> alerts) {
        if (value == null) {
            return;
        }
        if (value > 110) {
            alerts.add(critical("tensionDiastolique", value + " mmHg", "> 110 mmHg"));
        } else if (value > 90) {
            alerts.add(attention("tensionDiastolique", value + " mmHg", "> 90 mmHg"));
        }
    }

    private static void evaluatePulse(Integer value, List<AlertDraft> alerts) {
        if (value == null) {
            return;
        }
        if (value < 40 || value > 120) {
            alerts.add(critical("pouls", value + " bpm", "< 40 ou > 120 bpm"));
        } else if (value < 50 || value > 100) {
            alerts.add(attention("pouls", value + " bpm", "< 50 ou > 100 bpm"));
        }
    }

    private static void evaluateSpo2(BigDecimal value, List<AlertDraft> alerts) {
        if (value == null) {
            return;
        }
        double spo2 = value.doubleValue();
        if (spo2 < 90) {
            alerts.add(critical("spO2", value + " %", "< 90 %"));
        } else if (spo2 < 94) {
            alerts.add(attention("spO2", value + " %", "< 94 %"));
        }
    }

    private static void evaluateRespiratoryRate(Integer value, List<AlertDraft> alerts) {
        if (value == null) {
            return;
        }
        if (value < 8 || value > 30) {
            alerts.add(critical("freqRespiratoire", value + " /min", "< 8 ou > 30 /min"));
        } else if (value < 12 || value > 20) {
            alerts.add(attention("freqRespiratoire", value + " /min", "< 12 ou > 20 /min"));
        }
    }

    private static AlertDraft attention(String parameter, String measured, String threshold) {
        return new AlertDraft(parameter, measured, threshold, VitalSignAlertLevel.ATTENTION);
    }

    private static AlertDraft critical(String parameter, String measured, String threshold) {
        return new AlertDraft(parameter, measured, threshold, VitalSignAlertLevel.CRITIQUE);
    }
}

package com.afya.platform.bff.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityReportFallbackTest {

  @Test
  void empty_marksResponseAsDegraded() {
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-31T23:59:59Z");

    var report = ActivityReportFallback.empty(from, to);

    assertThat(report.degraded()).isTrue();
    assertThat(report.notice()).contains("audit");
    assertThat(report.totalEvents()).isZero();
    assertThat(report.from()).isEqualTo(from);
    assertThat(report.to()).isEqualTo(to);
    assertThat(report.byAction()).isEmpty();
  }
}

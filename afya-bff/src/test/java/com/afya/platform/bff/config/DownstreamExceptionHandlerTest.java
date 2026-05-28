package com.afya.platform.bff.config;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamExceptionHandlerTest {

  private final DownstreamExceptionHandler handler =
      new DownstreamExceptionHandler(JsonMapper.builder().build());

  @Test
  void handleUnreachable_returns503WithClearMessage() {
    var response =
        handler.handleUnreachable(new ResourceAccessException("Connection refused"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message())
        .contains("Service indisponible")
        .contains("microservice");
  }
}

package com.ieltscreator.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

  private final CorsConfig corsConfig = new CorsConfig();

  @Test
  void registersAllowedOriginsForApiPaths() {
    CorsConfigurationSource source =
        corsConfig.corsConfigurationSource(new String[] {"http://localhost:3000"});

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/dashboard/summary");
    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:3000");
    assertThat(configuration.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
  }

  @Test
  void doesNotRegisterConfigurationForNonApiPaths() {
    CorsConfigurationSource source =
        corsConfig.corsConfigurationSource(new String[] {"http://localhost:3000"});

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/actuator/health");

    assertThat(source.getCorsConfiguration(request)).isNull();
  }
}

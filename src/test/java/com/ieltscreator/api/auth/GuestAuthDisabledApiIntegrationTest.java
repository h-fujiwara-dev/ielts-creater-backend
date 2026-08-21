package com.ieltscreator.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.common.web.ErrorResponse;
import com.ieltscreator.api.support.AbstractCognitoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/** {@code app.guest.enabled=false}時、guest-tokenエンドポイントが503を返し一時的に無効化できることを検証する。 */
@TestPropertySource(properties = "app.guest.enabled=false")
class GuestAuthDisabledApiIntegrationTest extends AbstractCognitoIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void returnsServiceUnavailableWhenGuestModeIsDisabled() {
    var response =
        restTemplate.postForEntity("/api/v1/auth/guest-token", null, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().error()).isEqualTo("GUEST_MODE_DISABLED");
  }
}

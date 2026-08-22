package com.ieltscreator.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.auth.dto.GuestTokenResponse;
import com.ieltscreator.api.common.web.ErrorResponse;
import com.ieltscreator.api.support.AbstractCognitoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

/**
 * {@code app.guest.enabled}既定値(true)のまま、{@code POST /api/v1/auth/guest-token}
 * エンドポイント自体を検証する。無効化時の挙動は{@link GuestAuthDisabledApiIntegrationTest}で検証する。
 */
class GuestAuthApiIntegrationTest extends AbstractCognitoIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @MockitoBean private CognitoIdentityProviderClient cognitoIdentityProviderClient;

  @Test
  void issuesGuestTokenWhenGuestModeEnabled() {
    when(cognitoIdentityProviderClient.initiateAuth(any(InitiateAuthRequest.class)))
        .thenReturn(
            InitiateAuthResponse.builder()
                .authenticationResult(
                    AuthenticationResultType.builder()
                        .accessToken("guest-access-token")
                        .idToken("guest-id-token")
                        .expiresIn(3600)
                        .build())
                .build());

    GuestTokenResponse response =
        restTemplate.postForObject("/api/v1/auth/guest-token", null, GuestTokenResponse.class);

    assertThat(response.accessToken()).isEqualTo("guest-access-token");
    assertThat(response.idToken()).isEqualTo("guest-id-token");
    assertThat(response.expiresIn()).isEqualTo(3600);
  }

  @Test
  void returnsServiceUnavailableWhenCognitoAuthFails() {
    when(cognitoIdentityProviderClient.initiateAuth(any(InitiateAuthRequest.class)))
        .thenThrow(
            CognitoIdentityProviderException.builder()
                .message("Incorrect username or password.")
                .build());

    var response =
        restTemplate.postForEntity("/api/v1/auth/guest-token", null, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().error()).isEqualTo("GUEST_AUTH_FAILED");
  }
}

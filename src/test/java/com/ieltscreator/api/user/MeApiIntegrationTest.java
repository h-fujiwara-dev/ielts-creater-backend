package com.ieltscreator.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.support.AbstractCognitoIntegrationTest;
import com.ieltscreator.api.user.dto.MeResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MeApiIntegrationTest extends AbstractCognitoIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private AppUserRepository appUserRepository;

  @MockitoBean private CognitoUserAttributesClient cognitoUserAttributesClient;

  @Test
  void rejectsRequestWithoutToken() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/me", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsTokenIssuedForAnotherAppClient() {
    String token =
        signedAccessToken("sub-wrong-client", OTHER_CLIENT_ID, Instant.now().plusSeconds(3600));

    ResponseEntity<String> response =
        restTemplate.exchange("/api/v1/me", HttpMethod.GET, authorized(token), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsExpiredToken() {
    String token = signedAccessToken("sub-expired", CLIENT_ID, Instant.now().minusSeconds(60));

    ResponseEntity<String> response =
        restTemplate.exchange("/api/v1/me", HttpMethod.GET, authorized(token), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void provisionsAppUserOnFirstAccessWithValidToken() {
    String subject = "sub-" + UUID.randomUUID();
    when(cognitoUserAttributesClient.fetch(anyString()))
        .thenReturn(new CognitoUserAttributes("newuser@example.com", "New User"));
    String token = signedAccessToken(subject, CLIENT_ID, Instant.now().plusSeconds(3600));

    ResponseEntity<MeResponse> response =
        restTemplate.exchange("/api/v1/me", HttpMethod.GET, authorized(token), MeResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().email()).isEqualTo("newuser@example.com");
    assertThat(response.getBody().displayName()).isEqualTo("New User");
    assertThat(response.getBody().isGuest()).isFalse();
    assertThat(appUserRepository.findByCognitoSub(subject)).isPresent();
  }

  private HttpEntity<Void> authorized(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }
}

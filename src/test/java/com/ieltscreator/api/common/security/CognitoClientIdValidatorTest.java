package com.ieltscreator.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CognitoClientIdValidatorTest {

  private final CognitoClientIdValidator validator =
      new CognitoClientIdValidator("expected-client-id");

  @Test
  void succeedsWhenClientIdMatches() {
    assertThat(validator.validate(jwtWithClientId("expected-client-id")).hasErrors()).isFalse();
  }

  @Test
  void failsWhenClientIdDoesNotMatch() {
    assertThat(validator.validate(jwtWithClientId("other-client-id")).hasErrors()).isTrue();
  }

  @Test
  void failsWhenClientIdClaimIsMissing() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("sub-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    assertThat(validator.validate(jwt).hasErrors()).isTrue();
  }

  private Jwt jwtWithClientId(String clientId) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim("client_id", clientId)
        .subject("sub-123")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}

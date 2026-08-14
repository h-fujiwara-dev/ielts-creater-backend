package com.ieltscreator.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CognitoTokenUseValidatorTest {

  private final CognitoTokenUseValidator validator = new CognitoTokenUseValidator();

  @Test
  void succeedsWhenTokenUseIsAccess() {
    assertThat(validator.validate(jwtWithTokenUse("access")).hasErrors()).isFalse();
  }

  @Test
  void failsWhenTokenUseIsIdToken() {
    assertThat(validator.validate(jwtWithTokenUse("id")).hasErrors()).isTrue();
  }

  @Test
  void failsWhenTokenUseClaimIsMissing() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("sub-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    assertThat(validator.validate(jwt).hasErrors()).isTrue();
  }

  private Jwt jwtWithTokenUse(String tokenUse) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim("token_use", tokenUse)
        .subject("sub-123")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}

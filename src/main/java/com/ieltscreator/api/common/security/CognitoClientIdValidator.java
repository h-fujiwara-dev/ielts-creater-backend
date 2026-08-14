package com.ieltscreator.api.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * CognitoアクセストークンはOIDC標準のaudクレームを持たないため、代わりにclient_idクレームが設定済みの App Client IDと一致することを検証する（API一覧.md
 * 3章）。
 */
public class CognitoClientIdValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2Error INVALID_CLIENT_ID =
      new OAuth2Error("invalid_token", "client_id does not match the configured App Client.", null);

  private final String expectedClientId;

  public CognitoClientIdValidator(String expectedClientId) {
    this.expectedClientId = expectedClientId;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    if (!expectedClientId.equals(token.getClaimAsString("client_id"))) {
      return OAuth2TokenValidatorResult.failure(INVALID_CLIENT_ID);
    }
    return OAuth2TokenValidatorResult.success();
  }
}

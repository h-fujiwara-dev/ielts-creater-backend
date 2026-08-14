package com.ieltscreator.api.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** token_useクレームが"access"であることを検証する。IDトークンが誤ってAPI呼び出しに使われるのを防ぐ （API一覧.md 3章）。 */
public class CognitoTokenUseValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2Error INVALID_TOKEN_USE =
      new OAuth2Error("invalid_token", "token_use must be \"access\".", null);

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    if (!"access".equals(token.getClaimAsString("token_use"))) {
      return OAuth2TokenValidatorResult.failure(INVALID_TOKEN_USE);
    }
    return OAuth2TokenValidatorResult.success();
  }
}

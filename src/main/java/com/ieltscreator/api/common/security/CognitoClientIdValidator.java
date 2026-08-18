package com.ieltscreator.api.common.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * CognitoアクセストークンはOIDC標準のaudクレームを持たないため、代わりにclient_idクレームが設定済みの App Client
 * IDのいずれかと一致することを検証する（API一覧.md 3章）。通常ユーザー用（web）とゲスト用 （guest、#00056）の2つのApp Clientを許可対象とする。
 */
public class CognitoClientIdValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2Error INVALID_CLIENT_ID =
      new OAuth2Error("invalid_token", "client_id does not match any configured App Client.", null);

  private final Set<String> allowedClientIds;

  public CognitoClientIdValidator(Collection<String> allowedClientIds) {
    this.allowedClientIds = Set.copyOf(allowedClientIds);
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    String clientId = token.getClaimAsString("client_id");
    // allowedClientIdsはSet.copyOf由来の不変集合でcontains(null)がNPEを送出するため先に弾く
    if (clientId == null || !allowedClientIds.contains(clientId)) {
      return OAuth2TokenValidatorResult.failure(INVALID_CLIENT_ID);
    }
    return OAuth2TokenValidatorResult.success();
  }
}

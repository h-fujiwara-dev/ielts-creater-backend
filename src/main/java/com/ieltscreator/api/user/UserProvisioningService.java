package com.ieltscreator.api.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * 認証済みJWTから{@code app_user}をUpsertする。GetUser API呼び出しは初回アクセス時のみ行い（API一覧.md
 * 3章「初回のみapp_userを自動作成」）、2回目以降は既存行をそのまま返す。全リクエストで毎回Cognitoへ 問い合わせるとレイテンシ・レート制限の観点で不利なため。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class UserProvisioningService {

  private final AppUserRepository appUserRepository;
  private final CognitoUserAttributesClient cognitoUserAttributesClient;

  public AppUser provisionFromToken(Jwt jwt) {
    return appUserRepository
        .findByCognitoSub(jwt.getSubject())
        .orElseGet(() -> createUser(jwt.getSubject(), jwt.getTokenValue()));
  }

  private AppUser createUser(String cognitoSub, String accessToken) {
    CognitoUserAttributes attributes = cognitoUserAttributesClient.fetch(accessToken);
    AppUser newUser =
        AppUser.builder()
            .cognitoSub(cognitoSub)
            .email(attributes.email())
            .displayName(attributes.displayName())
            .build();
    try {
      return appUserRepository.save(newUser);
    } catch (DataIntegrityViolationException e) {
      // 同一ユーザーからの初回リクエストが同時に競合しUNIQUE制約(cognito_sub)に違反した場合、
      // 既に別スレッドが作成した行を再取得して返す。
      return appUserRepository.findByCognitoSub(cognitoSub).orElseThrow(() -> e);
    }
  }
}

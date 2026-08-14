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
            .displayName(resolveDisplayName(attributes))
            .build();
    try {
      return appUserRepository.save(newUser);
    } catch (DataIntegrityViolationException e) {
      // 同一ユーザーからの初回リクエストが同時に競合しUNIQUE制約(cognito_sub)に違反した場合、
      // 既に別スレッドが作成した行を再取得して返す。
      return appUserRepository.findByCognitoSub(cognitoSub).orElseThrow(() -> e);
    }
  }

  // Cognito Hosted UIの標準サインアップ画面はemail・passwordのみを収集し、name属性を設定しない。
  // GET /api/v1/meのdisplayNameは必須文字列がレスポンス契約（API設計書/GET_me.md）のため、
  // name未設定時はemailのローカル部をフォールバックとして使う（#00046）。
  private static String resolveDisplayName(CognitoUserAttributes attributes) {
    if (attributes.displayName() != null && !attributes.displayName().isBlank()) {
      return attributes.displayName();
    }
    String email = attributes.email();
    int atIndex = email.indexOf('@');
    return atIndex > 0 ? email.substring(0, atIndex) : email;
  }
}

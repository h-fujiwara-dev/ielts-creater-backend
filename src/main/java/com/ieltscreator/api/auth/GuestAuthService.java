package com.ieltscreator.api.auth;

import com.ieltscreator.api.auth.dto.GuestTokenResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

/**
 * 共有デモアカウント（ゲスト、#00056）をInitiateAuth（USER_PASSWORD_AUTH）でプログラム的に認証する。 ゲスト用App Client（infraリポジトリ
 * terraform/modules/cognito の"guest"クライアント）は ALLOW_USER_PASSWORD_AUTHのみを許可しHosted UI/OAuthは使わない。
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class GuestAuthService {

  private final CognitoIdentityProviderClient cognitoClient;
  private final String guestClientId;
  private final String guestUsername;
  private final String guestPassword;

  public GuestAuthService(
      CognitoIdentityProviderClient cognitoClient,
      @Value("${app.guest.cognito.client-id}") String guestClientId,
      @Value("${app.guest.cognito.username}") String guestUsername,
      @Value("${app.guest.cognito.password}") String guestPassword) {
    this.cognitoClient = cognitoClient;
    this.guestClientId = guestClientId;
    this.guestUsername = guestUsername;
    this.guestPassword = guestPassword;
  }

  public GuestTokenResponse issueToken() {
    try {
      InitiateAuthResponse response =
          cognitoClient.initiateAuth(
              InitiateAuthRequest.builder()
                  .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                  .clientId(guestClientId)
                  .authParameters(Map.of("USERNAME", guestUsername, "PASSWORD", guestPassword))
                  .build());
      AuthenticationResultType result = response.authenticationResult();
      return new GuestTokenResponse(result.accessToken(), result.idToken(), result.expiresIn());
    } catch (CognitoIdentityProviderException e) {
      log.error("Guest InitiateAuth failed: {}", e.getMessage(), e);
      throw new GuestAuthFailedException("Failed to issue a guest token.", e);
    }
  }
}

package com.ieltscreator.api.auth;

import com.ieltscreator.api.auth.dto.GuestTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * frontendのNextAuth Credentialsプロバイダー（id: guest）から呼ばれる未認証エンドポイント（CognitoSecurityConfigでpermitAll）。
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class GuestAuthController {

  private final GuestAuthService guestAuthService;
  private final boolean guestEnabled;

  public GuestAuthController(
      GuestAuthService guestAuthService, @Value("${app.guest.enabled:true}") boolean guestEnabled) {
    this.guestAuthService = guestAuthService;
    this.guestEnabled = guestEnabled;
  }

  @PostMapping("/guest-token")
  public GuestTokenResponse issueGuestToken() {
    if (!guestEnabled) {
      throw new GuestModeDisabledException();
    }
    return guestAuthService.issueToken();
  }
}

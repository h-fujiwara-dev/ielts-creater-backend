package com.ieltscreator.api.user;

import com.ieltscreator.api.user.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phase2（Cognito認証）向け。no-authモードではJWTが存在しないため、cognitoモード時のみ有効化する。 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class MeController {

  private final UserProvisioningService userProvisioningService;

  @GetMapping
  public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
    return AppUserMapper.toMeResponse(userProvisioningService.provisionFromToken(jwt));
  }
}

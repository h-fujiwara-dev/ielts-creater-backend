package com.ieltscreator.api.user;

import com.ieltscreator.api.common.security.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Phase2（Cognito認証）向け。認証済みJWTから{@code app_user}を自動プロビジョニングしIDを返す。
 * UserProvisioningService（user機能パッケージ）に依存するため、common/security配下ではなくuser配下に 置く（実装規約.md
 * 2.1「commonから各機能パッケージへの依存は禁止」）。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class CognitoCurrentUserProvider implements CurrentUserProvider {

  private final UserProvisioningService userProvisioningService;

  @Override
  public UUID currentUserId() {
    Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return userProvisioningService.provisionFromToken(jwt).getId();
  }
}

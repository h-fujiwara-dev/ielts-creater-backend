package com.ieltscreator.api.common.security;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Phase1（認証なし）向け。V2__seed_dev_user.sql で投入した固定devユーザーのIDを返す。 */
@Component
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "no-auth")
public class NoAuthCurrentUserProvider implements CurrentUserProvider {

  public static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Override
  public UUID currentUserId() {
    return DEV_USER_ID;
  }
}

package com.ieltscreator.api.common.security;

import java.util.UUID;

public interface CurrentUserProvider {

  UUID currentUserId();
}

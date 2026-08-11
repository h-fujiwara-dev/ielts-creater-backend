package com.ieltscreator.api.user;

import com.ieltscreator.api.user.dto.MeResponse;

public final class AppUserMapper {

  private AppUserMapper() {}

  public static MeResponse toMeResponse(AppUser appUser) {
    return new MeResponse(appUser.getId(), appUser.getEmail(), appUser.getDisplayName());
  }
}

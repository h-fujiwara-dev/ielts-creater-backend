package com.ieltscreator.api.auth;

/** {@code app.guest.enabled=false}のとき、ゲストログインを一時的に無効化するために投げる。 */
public class GuestModeDisabledException extends RuntimeException {

  public GuestModeDisabledException() {
    super("Guest login is currently disabled.");
  }
}

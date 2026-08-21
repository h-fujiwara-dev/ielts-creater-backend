package com.ieltscreator.api.auth;

/** Cognito InitiateAuthの呼び出しに失敗した場合（サービス側障害・設定不備等）に投げる。 */
public class GuestAuthFailedException extends RuntimeException {

  public GuestAuthFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}

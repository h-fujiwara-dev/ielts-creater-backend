package com.ieltscreator.api.auth.dto;

/** GuestAuthServiceがCognito InitiateAuth（USER_PASSWORD_AUTH）から取得したトークンをそのまま公開する。 */
public record GuestTokenResponse(String accessToken, String idToken, Integer expiresIn) {}

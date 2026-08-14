package com.ieltscreator.api.user;

/** Cognito GetUser APIから取得したプロフィール属性。 */
public record CognitoUserAttributes(String email, String displayName) {}

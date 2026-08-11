package com.ieltscreator.api.user;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

/**
 * Cognitoアクセストークンには標準ではemailクレームが含まれないため、GetUser APIでプロフィール属性を
 * 取得する。GetUserはユーザー自身のアクセストークンを渡すだけで呼べる操作でAWS SigV4署名は不要なため
 * AnonymousCredentialsProviderで構成する（呼び出しにはApp Clientのスコープに aws.cognito.signin.user.admin
 * が必要。infraリポジトリ terraform/modules/cognito 参照）。
 */
@Component
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class CognitoUserAttributesClient {

  private final CognitoIdentityProviderClient cognitoClient;

  public CognitoUserAttributesClient(@Value("${app.auth.cognito.region}") String region) {
    this.cognitoClient =
        CognitoIdentityProviderClient.builder()
            .region(Region.of(region))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build();
  }

  public CognitoUserAttributes fetch(String accessToken) {
    GetUserResponse response =
        cognitoClient.getUser(GetUserRequest.builder().accessToken(accessToken).build());
    Map<String, String> attributes =
        response.userAttributes().stream()
            .collect(Collectors.toMap(AttributeType::name, AttributeType::value, (a, b) -> a));
    return new CognitoUserAttributes(attributes.get("email"), attributes.get("name"));
  }
}

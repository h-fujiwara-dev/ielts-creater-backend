package com.ieltscreator.api.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * ゲスト機能（#00056）のGuestAuthServiceがInitiateAuthを呼ぶために使う{@link
 * CognitoIdentityProviderClient}。InitiateAuthはユーザー自身の資格情報のみで呼べる操作でAWS SigV4署名は
 * 不要なため、実IAM認証情報を要するDefaultCredentialsProviderではなくAnonymousCredentialsProviderで構成する （{@link
 * com.ieltscreator.api.user.CognitoUserAttributesClient}のGetUser呼び出しと同様）。
 */
@Configuration
public class CognitoIdentityProviderClientConfig {

  @Bean
  @ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
  public CognitoIdentityProviderClient cognitoIdentityProviderClient(
      @Value("${app.auth.cognito.region}") String region) {
    return CognitoIdentityProviderClient.builder()
        .region(Region.of(region))
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .build();
  }
}

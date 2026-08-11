package com.ieltscreator.api.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.polly.PollyClient;

/**
 * Amazon Polly呼び出し用の{@link PollyClient}（実装規約.md 3.4章）。認証情報・regionはAWS SDKの
 * デフォルトプロバイダチェーンに委ねる（ローカルは環境変数、本番はECS IAM Role）。
 */
@Configuration
@RequiredArgsConstructor
public class PollyClientConfig {

  private final PollyProperties properties;

  @Bean
  @ConditionalOnProperty(prefix = "app.generation", name = "mode", havingValue = "openai")
  public PollyClient pollyClient() {
    return PollyClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .overrideConfiguration(
            builder ->
                builder
                    .apiCallTimeout(properties.getApiCallTimeout())
                    .apiCallAttemptTimeout(properties.getApiCallAttemptTimeout())
                    .retryStrategy(retry -> retry.maxAttempts(properties.getMaxAttempts())))
        .build();
  }
}

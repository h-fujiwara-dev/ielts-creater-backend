package com.ieltscreator.api.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Listening音声保存用の{@link S3Client}（実装規約.md 2章 S3StorageService、#00044）。{@link
 * PollyClientConfig}と同じく認証情報・regionはAWS SDKのデフォルトプロバイダチェーンに委ねる（ローカルは環境変数、本番はECS IAM Role）。
 */
@Configuration
public class S3ClientConfig {

  @Bean
  @ConditionalOnProperty(prefix = "app.storage", name = "mode", havingValue = "s3")
  public S3Client s3Client() {
    return S3Client.builder()
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .build();
  }
}

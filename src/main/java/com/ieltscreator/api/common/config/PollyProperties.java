package com.ieltscreator.api.common.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Amazon Polly呼び出しの設定値（実装規約.md 3.4章、R-3の確定値）。認証情報・regionはAWS SDKのデフォルトプロバイダチェーン（ローカルは環境変数、本番はECS
 * IAM Role）に委ね、ここでは持たない。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.polly")
public class PollyProperties {

  private Duration apiCallAttemptTimeout = Duration.ofSeconds(10);
  private Duration apiCallTimeout = Duration.ofSeconds(25);
  private int maxAttempts = 3;
}

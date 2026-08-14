package com.ieltscreator.api.common.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** OpenAI Structured Outputs呼び出しの設定値（実装規約.md 3.4章、R-3の確定値）。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

  private String apiKey;
  private String baseUrl = "https://api.openai.com/v1";
  private String model = "gpt-4o-mini";
  private Duration connectTimeout = Duration.ofSeconds(5);
  private Duration readTimeout = Duration.ofSeconds(60);
  private int maxAttempts = 2;
  private Duration retryBackoff = Duration.ofSeconds(2);
}

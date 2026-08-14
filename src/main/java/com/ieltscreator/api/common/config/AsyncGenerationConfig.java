package com.ieltscreator.api.common.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 問題生成のバックグラウンド実行基盤（実装規約8章）。202 Acceptedで即時応答するため、 リクエストスレッドをブロックせずVirtual Thread
 * Executorへ生成処理を委譲する。
 */
@Configuration
public class AsyncGenerationConfig {

  @Bean
  public ExecutorService questionSetGenerationExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}

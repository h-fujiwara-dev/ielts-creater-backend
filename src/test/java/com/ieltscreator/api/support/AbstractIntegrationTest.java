package com.ieltscreator.api.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers上のPostgreSQLを使った結合テストの共通基盤。app.auth.modeはno-authに固定し、
 * localプロファイルは有効化しない（db/dev-migrationのシードは読み込まず、フィクスチャで独自にデータを用意する）。
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.auth.mode=no-auth")
public abstract class AbstractIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}

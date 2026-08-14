package com.ieltscreator.api.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers上のPostgreSQLを使った結合テストの共通基盤。app.auth.modeはno-authに固定し、
 * localプロファイルは有効化しない（db/dev-migrationのシードは読み込まず、フィクスチャで独自にデータを用意する）。
 *
 * <p>複数の結合テストクラスがこの基底クラスを継承するため、Testcontainers公式の「シングルトンコンテナ」パターンを採る:
 * {@code @Container}を付けず（JUnit5の自動ライフサイクル管理に委ねない）、static初期化ブロックで1回だけ起動する。
 * {@code @Container}を付けたままだと、クラスごとに{@code afterAll}でコンテナが停止・再起動されてポートが変わる一方、
 * Spring側はテストクラス間でキャッシュした古いポートのDataSourceを使い回してしまい、2つ目以降のテストクラスが 接続不能になる（コンテナはJVM終了時にRyukが破棄する）。
 */
@Tag("integration")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.auth.mode=no-auth")
public abstract class AbstractIntegrationTest {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    POSTGRES.start();
  }
}

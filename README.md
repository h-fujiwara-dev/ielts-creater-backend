# IELTS Creator — Backend

[IELTS Creator](https://github.com/h-fujiwara-dev/ielts-creater)（AIによるIELTS練習問題作成アプリ）のバックエンドAPI。Spring Boot 3 + Java 21で実装。

プロジェクト全体の概要・業務/システム要件・アーキテクチャは[ielts-createrリポジトリ（ランディング）](https://github.com/h-fujiwara-dev/ielts-creater)を参照してください。フロントエンドは[ielts-creater-frontend](https://github.com/h-fujiwara-dev/ielts-creater-frontend)、インフラは[ielts-creater-infra](https://github.com/h-fujiwara-dev/ielts-creater-infra)にあります。

## ドキュメント

- [docs/実装規約.md](./docs/実装規約.md) — Spring Boot基盤の実装規約（パッケージ構成・命名規約・ビルド構成・テスト方針等）
- [docs/API一覧.md](./docs/API一覧.md) — エンドポイント一覧、共通仕様（認証フロー・エラー形式・ロギング方針）、実装構成
- [docs/API設計書/](./docs/API設計書/) — エンドポイントごとの詳細仕様（生成フロー・AI生成設計・外部連携・採点ロジックを関連エンドポイントに統合）
- [docs/ER図・テーブル定義.md](./docs/ER図・テーブル定義.md) — ER図・テーブル定義書

## ローカル開発

```bash
# 1. ローカルPostgresを起動
docker compose up -d

# 2. APIを起動（http://localhost:8080）。localプロファイルでPhase1のno-auth・固定devユーザーとして動作する
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

# 3. 起動確認
curl http://localhost:8080/actuator/health
```

Phase 1では認証なし・固定devユーザーで動作するため、Cognito/AWS連携前でも「生成→回答→採点」の一連の流れをローカルで確認できます。

### その他の開発コマンド

```bash
./gradlew spotlessCheck   # コードフォーマットチェック（CI必須チェック）
./gradlew test            # Unit Test（CI必須チェック、Docker不要）
./gradlew integrationTest # Integration Test（Testcontainers利用、Docker必須）
```

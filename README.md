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

### 環境変数（OpenAI/Polly連携）

問題生成（`POST /question-sets`）のAI・音声合成連携はstub実装と実装（`app.generation.mode: stub|openai`）を設定値で切り替えられる。全環境共通でstubが既定（APIコスト・外部依存なし）のため、`OPENAI_API_KEY`等の環境変数なしでも動作する。実際にOpenAI/Pollyへ接続して確認したい場合は、[.env.example](./.env.example)を参考に環境変数を設定した上で`APP_GENERATION_MODE=openai`を一時的に指定して起動する。

```bash
# .env.example を参考に OPENAI_API_KEY / AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION をexportした上で
APP_GENERATION_MODE=openai SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### その他の開発コマンド

```bash
./gradlew spotlessCheck   # コードフォーマットチェック（CI必須チェック）
./gradlew test            # Unit Test（CI必須チェック、Docker不要）
./gradlew integrationTest # Integration Test（Testcontainers利用、Docker必須）
```

## Dockerイメージのビルド・ECR push（dev環境、#00044）

prodプロファイル（Supabase接続・S3StorageService・Cognito認証）で動作する本番向けイメージは`Dockerfile`（マルチステージビルド）でビルドする。ECS Fargateへのデプロイ手順は[ielts-creater-infra README](https://github.com/h-fujiwara-dev/ielts-creater-infra#backend-awsインフラの構築手順dev環境)を参照。

```bash
docker build -t ielts-creater-api:local .

# ECRへpushする場合（<ecr_repository_url>はielts-creater-infraのterraform outputを使用）
aws ecr get-login-password --region ap-northeast-1 | \
  docker login --username AWS --password-stdin <ecr_repository_url を : で分割した左側>
docker tag ielts-creater-api:local <ecr_repository_url>:latest
docker push <ecr_repository_url>:latest
```

## prod環境への継続的デプロイ（CI/CD、#00050）

`main`ブランチへのpush（リリースマージ）をトリガーに、[`.github/workflows/deploy-prod.yml`](.github/workflows/deploy-prod.yml)がDockerイメージのビルド・ECR push・ECSタスク定義の新リビジョン登録・ECSサービス更新までを自動実行する。

AWS認証は長期のAccess KeyをGitHub Secretsに置かず、OIDC federationで`ielts-creater-infra`（`envs/prod`）が定義するIAM Role（`ielts-creater-prod-github-actions-deploy`）を実行時に一時的に引き受ける方式（信頼関係はこのリポジトリの`main`ブランチのワークフローのみに限定）。

`envs/prod`を`terraform apply`した後、以下をこのリポジトリのGitHub repository variables（Settings > Secrets and variables > Actions > Variables）に設定しておく必要がある。

| Variable | 値の取得元 |
| --- | --- |
| `AWS_DEPLOY_ROLE_ARN` | `terraform output github_actions_deploy_role_arn` |
| `AWS_REGION` | `terraform output`の`aws_region`（既定 `ap-northeast-1`） |
| `ECR_REPOSITORY` | `terraform output ecr_repository_url`のリポジトリ名部分 |
| `ECS_CLUSTER` | `terraform output ecs_cluster_name` |
| `ECS_SERVICE` | `terraform output ecs_service_name` |
| `ECS_TASK_FAMILY` | `ECS_CLUSTER`と同じ値（envs/prodのタスク定義familyはクラスタ名と同一の命名規則） |

# CLAUDE.md

簡易版。後ほどブラッシュアップ予定。

## このリポジトリについて

- [IELTS Creator](https://github.com/h-fujiwara-dev/ielts-creater) のバックエンドAPI
- Spring Boot 3 + Java 21
- プロジェクト全体の要件・アーキテクチャは[ielts-createrリポジトリ](https://github.com/h-fujiwara-dev/ielts-creater)を参照

## 言語ポリシー

- 日本企業向けポートフォリオとして公開するため、**ドキュメント・コメント・コミットメッセージは日本語**で記載する
- コード上の識別子（変数名・メソッド名・クラス名・パッケージ名等）は英語のまま実装する

## コミットメッセージ規則

- 書式: `[#チケット番号] type: 概要（日本語）`（例: `[#00001] feat: リポジトリ新規構築`）
- チケット番号は5桁ゼロ埋め。typeはConventional Commits準拠（feat/fix/docs/style/refactor/test/chore/perf/build/ci/revert）
- ielts-creater / -frontend / -backend / -infra の4リポジトリ共通のルール
- テンプレートは `.gitmessage`（`git config commit.template .gitmessage` で有効化済み。cloneし直した場合は再設定が必要）
- チケット番号は[ielts-createrリポジトリ tickets/](https://github.com/h-fujiwara-dev/ielts-creater/blob/main/tickets/)で採番・管理する

## ドキュメント

- [docs/API一覧.md](./docs/API一覧.md) — API一覧・共通仕様（認証フロー・エラー形式・ロギング方針）・実装構成
- [docs/API設計書/](./docs/API設計書/) — エンドポイントごとの詳細仕様
- [docs/ER図・テーブル定義.md](./docs/ER図・テーブル定義.md) — ER図・テーブル定義

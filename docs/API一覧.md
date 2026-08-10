# API一覧

- 文書種別: API一覧・共通仕様
- 対象プロジェクト: IELTS Creator（IELTS練習問題作成アプリ／ポートフォリオ用途）
- 更新日: 2026-08-10（付録の実装構成を実装規約.mdへ集約）
- 関連文書: [システム要件定義書（ielts-createrリポジトリ）8章 アーキテクチャ](https://github.com/h-fujiwara-dev/ielts-creater/blob/main/docs/システム要件定義書.md#8-アーキテクチャ) / [実装規約.md](./実装規約.md) / [API設計書/](./API設計書/) / [ER図・テーブル定義](./ER図・テーブル定義.md)

## 1. API一覧

全エンドポイントは`Authorization: Bearer <Cognito AccessToken>`必須（`/actuator/health`除く）。

| メソッド | パス | 説明 | 詳細 |
| --- | --- | --- | --- |
| GET | `/api/v1/me` | JWTから自動プロビジョニングしたユーザー情報取得 | [API設計書](./API設計書/GET_me.md) |
| POST | `/api/v1/question-sets` | 問題セット生成を開始（202 Accepted） | [API設計書](./API設計書/POST_question-sets.md) |
| GET | `/api/v1/question-sets/{id}` | 問題セット詳細取得（正解は含めない） | [API設計書](./API設計書/GET_question-sets-id.md) |
| GET | `/api/v1/question-sets/{id}/audio-segments` | Listening用、署名付きURL付きセグメント一覧 | [API設計書](./API設計書/GET_question-sets-id-audio-segments.md) |
| POST | `/api/v1/attempts` | 受験（Attempt）を開始 | [API設計書](./API設計書/POST_attempts.md) |
| PATCH | `/api/v1/attempts/{id}/answers` | 回答の部分保存 | [API設計書](./API設計書/PATCH_attempts-id-answers.md) |
| GET | `/api/v1/attempts/{id}/answers` | 保存済みの回答一覧取得（未提出分の復元用） | [API設計書](./API設計書/GET_attempts-id-answers.md) |
| POST | `/api/v1/attempts/{id}/submit` | 採点実行 | [API設計書](./API設計書/POST_attempts-id-submit.md) |
| GET | `/api/v1/attempts/{id}` | 採点済み結果の詳細 | [API設計書](./API設計書/GET_attempts-id.md) |
| GET | `/api/v1/attempts` | 受験履歴一覧（ページング・絞り込み） | [API設計書](./API設計書/GET_attempts.md) |
| GET | `/api/v1/dashboard/summary` | ダッシュボード集計データ | [API設計書](./API設計書/GET_dashboard-summary.md) |

各画面がどのAPIを呼ぶかは[frontendリポジトリ docs/画面一覧.md](https://github.com/h-fujiwara-dev/ielts-creater-frontend/blob/main/docs/画面一覧.md)を参照。

## 2. 共通仕様

- ベースパス: `/api/v1`
- 認証: `Authorization: Bearer <Cognito AccessToken>`（`/actuator/health`を除く）
- エラーレスポンス形式:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "difficulty must be one of BAND_4_5, BAND_5_6, BAND_6_7, BAND_7_8_PLUS",
  "timestamp": "2026-08-08T10:00:00Z"
}
```

### 2.1 例外とHTTPステータスの対応

| 例外 | HTTPステータス | 発生条件 |
| --- | --- | --- |
| `ResourceNotFoundException` | 404 | 指定IDの問題セット/受験が存在しない、または他ユーザーのものである |
| `ValidationException` | 400 | リクエストパラメータ不正（不正なsection/difficulty等） |
| `GenerationFailedException` | 422 | AI生成がリトライ上限に達し失敗 |
| `RateLimitExceededException` | 429 | ユーザーの1日あたり生成回数上限（2回）に達した |
| `UnauthorizedException` | 401 | JWT検証失敗・期限切れ |
| その他未捕捉例外 | 500 | `GlobalExceptionHandler`（`@RestControllerAdvice`）で共通エラーレスポンスに変換 |

## 3. 認証フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant Web as Next.js(NextAuth)
    participant Cognito as Amazon Cognito
    participant Api as Spring Boot API

    U->>Web: ログイン操作
    Web->>Cognito: Authorization Code + PKCE
    Cognito-->>Web: Access Token / ID Token
    Web->>Web: httpOnly暗号化Cookieにセッション保存
    U->>Web: 保護ページへアクセス
    Web->>Api: Authorization: Bearer <AccessToken>
    Api->>Cognito: JWKSでトークン検証
    Api->>Api: 初回のみapp_userを自動作成(Upsert)
    Api-->>Web: レスポンス
```

- CognitoのアクセストークンはOIDC標準の`aud`クレームを持たないため、バックエンドでは`token_use=access`と`client_id`の検証を独自実装で追加する
- Cognito App Client（confidential）はTerraform（[infraリポジトリ](https://github.com/h-fujiwara-dev/ielts-creater-infra)）で構築する
- フロントエンド（NextAuth.js側）の実装方針は[frontendリポジトリ docs/画面設計書/S-02_ログインサインアップ画面.md](https://github.com/h-fujiwara-dev/ielts-creater-frontend/blob/main/docs/画面設計書/S-02_ログインサインアップ画面.md)を参照

## 4. ロギング方針

- フォーマット: 構造化ログ（JSON）、CloudWatch Logsへ出力
- 必須フィールド: `timestamp`, `level`, `traceId`, `userId`（取得できる場合）, `message`
- 記録対象:
  - OpenAI/Polly呼び出しの開始・終了・所要時間・失敗理由（プロンプト全文はコスト・機密性の観点から本文はDEBUGレベルのみ）
  - 生成バリデーション失敗時のリトライ回数・最終結果
  - 採点処理のスコア確定ログ
  - 認証エラー（401/403）発生時のリクエストパス
- APIキー・トークン等の機密情報はログに出力しない

## 付録: 実装構成（パッケージ・クラス設計）

パッケージ構成（機能単位）・命名規約・主要クラスの責務は[実装規約.md 2章 パッケージ構成（機能単位）](./実装規約.md#2-パッケージ構成機能単位)を参照。

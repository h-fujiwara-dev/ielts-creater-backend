# POST /api/v1/auth/guest-token

- 関連文書: [API一覧](../API一覧.md) / [API一覧 3章 認証フロー](../API一覧.md#3-認証フロー) / [ER図・テーブル定義 app_user](../ER図・テーブル定義.md#31-app_user)

ゲスト機能（#00056）向けの未認証エンドポイント。共有デモアカウントの固定Cognito資格情報でInitiateAuth（`USER_PASSWORD_AUTH`）を実行し、トークンを発行する。frontendのNextAuth Credentialsプロバイダー（id: `guest`）から呼ばれる。

このエンドポイントは`CognitoSecurityConfig`で`permitAll()`（未認証で呼び出し可能）。`app.guest.enabled=false`の場合は503を返す。

リクエスト: ボディなし

レスポンス（200 OK）:

```json
{
  "accessToken": "eyJra...",
  "idToken": "eyJra...",
  "expiresIn": 3600
}
```

- `accessToken`はfrontendの以後のAPI呼び出しで`Authorization: Bearer`として使う。ゲスト用App Client（`explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH"]`のみ、Hosted UI/OAuthは使わない。[infraリポジトリ terraform/modules/cognito](https://github.com/h-fujiwara-dev/ielts-creater-infra/tree/main/terraform/modules/cognito)）で発行されるため、`client_id`クレームで通常ユーザーと判別できる
- ゲスト用Cognito資格情報（ユーザー名・パスワード・App Client ID）はSecrets Manager経由でECSにのみ注入される。Vercel（frontend）には配布しない

エラー:

| ステータス | エラーコード | 発生条件 |
| --- | --- | --- |
| 503 | `GUEST_MODE_DISABLED` | `app.guest.enabled=false` |
| 503 | `GUEST_AUTH_FAILED` | Cognito InitiateAuthの呼び出し失敗（サービス側障害・設定不備等） |

## ゲストの制約

- 問題生成（`POST /api/v1/question-sets`）のみ、IPアドレス単位で1日`app.guest.daily-ip-limit`（既定3）回までに制限される（`GuestQuotaInterceptor`、`guest_ip_quota`テーブル）。通常ユーザーのユーザーID単位の日次上限（`QuestionSetGenerationService`、[POST_question-sets.md](./POST_question-sets.md)参照）は共有デモアカウントに対してはバイパスされる
- クライアントIPの取得は`X-Client-Real-Ip`ヘッダー（優先）→`X-Forwarded-For`の先頭値→`request.getRemoteAddr()`の順にフォールバックする。dev環境実機確認により、API Gateway HTTP API（`HTTP_PROXY`統合）+ VPC Link + Cloud Map private integrationの構成では`X-Forwarded-For`が自動付与されず、`getRemoteAddr()`もVPC内部アドレスを返すため、いずれもクライアントの実IPを取得できないことが判明した。そのため`terraform/modules/api-gateway`側で`$context.http.sourceIp`（API Gateway自身が把握する実クライアントIP）を`X-Client-Real-Ip`ヘッダーとして`overwrite:`マッピングで注入させている（クライアントからのなりすまし送信は上書きされるため信頼できる）
- 生成された問題セット・受験履歴は通常ユーザーと同様にDBへ保存されるが、作成から`app.guest.retention-hours`（既定24時間）経過後に`GuestDataCleanupService`が定期削除する（Listening音声ファイルを含む）

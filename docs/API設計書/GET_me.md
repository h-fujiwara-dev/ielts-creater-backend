# GET /api/v1/me

- 関連文書: [API一覧](../API一覧.md) / [ER図・テーブル定義 app_user](../ER図・テーブル定義.md#31-app_user)

JWTから自動プロビジョニングしたログインユーザー情報を取得する。初回アクセス時は`UserProvisioningService`が`app_user`をUpsertする（[API一覧 3章 認証フロー](../API一覧.md#3-認証フロー)を参照）。

レスポンス（200 OK）:

```json
{
  "id": "u-01",
  "email": "user@example.com",
  "displayName": "Taro Yamada",
  "isGuest": false
}
```

- `isGuest`: ゲスト（#00056）の共有デモアカウントかどうか。`true`の場合、frontendは1日あたりの生成回数上限・データが約24時間で自動削除される旨を表示する

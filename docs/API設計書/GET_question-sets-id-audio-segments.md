# GET /api/v1/question-sets/{id}/audio-segments

- 関連文書: [API一覧](../API一覧.md) / [POST_question-sets.md（Polly連携）](./POST_question-sets.md#amazon-polly連携listening生成時) / [ER図・テーブル定義 audio_segment](../ER図・テーブル定義.md#35-audio_segment)

Listening用、署名付きURL付きの音声セグメント一覧を取得する。

レスポンス（200 OK）:

```json
{
  "segments": [
    { "turnIndex": 0, "url": "https://s3.../turn-0.mp3?X-Amz-...", "durationMs": 4200 }
  ]
}
```

## Amazon S3連携

- Listening音声ファイルを保存するバケットはBlock Public Accessを有効化し、非公開とする
- フロントへの配信は有効期限付き（15分程度）の署名付きURLで行う

## Phase1（ローカル保存）における配信

Phase1では`StorageService`実装を`LocalDiskStorageService`とし、S3は使用しない（[POST_question-sets.md 生成〜永続化の処理フロー](./POST_question-sets.md#生成永続化の処理フロー)参照）。`url`にはS3署名付きURLの代わりに以下の配信専用エンドポイントを指す相対パスを返す。

```text
GET /api/v1/question-sets/{id}/audio-segments/{audioSegmentId}/file
```

- 所有権確認（`{id}`が自ユーザーの問題セットであること）を行った上で、ローカル保存したファイルを`audio/wav`で返す
- Phase3でS3署名付きURLに差し替える際は、`url`の値のみが変わり、レスポンス全体の形状（`segments`配列の構造）は変更しない

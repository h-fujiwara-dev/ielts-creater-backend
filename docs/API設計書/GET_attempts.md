# GET /api/v1/attempts

- 関連文書: [API一覧](../API一覧.md) / [ER図・テーブル定義 attempt](../ER図・テーブル定義.md#310-attempt)

受験履歴一覧（ページング・絞り込み）を取得する。一覧の対象は`status=SUBMITTED`のAttemptのみとする（`IN_PROGRESS`の未提出Attemptは含めない）。

## クエリパラメータ

| パラメータ | 必須 | デフォルト | 説明 |
| --- | --- | --- | --- |
| `section` | 任意 | 指定なし（全件） | `READING` / `LISTENING`。`question_set.section`で絞り込む |
| `page` | 任意 | `0` | ページ番号（0始まり） |
| `size` | 任意 | `20` | 1ページあたりの件数 |

ソート順は`submitted_at DESC`固定（既存インデックス`idx_attempt_user_created`に対応）。

```json
{
  "items": [
    { "attemptId": "att-01", "questionSetId": "b3f1...", "section": "READING", "submittedAt": "2026-08-08T10:00:00Z", "rawScore": 7, "maxScore": 10 }
  ],
  "page": 0,
  "totalPages": 3
}
```

`questionSetId`は履歴一覧から結果画面・再挑戦画面へ遷移する際に使用する。

# GET /api/v1/attempts

- 関連文書: [API一覧](../API一覧.md) / [ER図・テーブル定義 attempt](../ER図・テーブル定義.md#310-attempt)

受験履歴一覧（ページング・絞り込み）を取得する。

```json
{
  "items": [
    { "attemptId": "att-01", "section": "READING", "submittedAt": "2026-08-08T10:00:00Z", "rawScore": 7, "maxScore": 10 }
  ],
  "page": 0,
  "totalPages": 3
}
```

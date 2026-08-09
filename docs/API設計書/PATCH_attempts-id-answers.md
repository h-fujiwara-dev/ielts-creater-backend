# PATCH /api/v1/attempts/{id}/answers

- 関連文書: [API一覧](../API一覧.md) / [POST_attempts-id-submit.md](./POST_attempts-id-submit.md) / [ER図・テーブル定義 attempt_answer](../ER図・テーブル定義.md#311-attempt_answer)

回答の部分保存（ユーザーの操作の都度呼び出される）。

```json
{
  "answers": [
    { "questionId": "q1", "userAnswerText": "TRUE" }
  ]
}
```

レスポンス: 204 No Content

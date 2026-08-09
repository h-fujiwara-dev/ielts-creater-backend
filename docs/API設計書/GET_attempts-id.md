# GET /api/v1/attempts/{id}

- 関連文書: [API一覧](../API一覧.md) / [POST_attempts-id-submit.md](./POST_attempts-id-submit.md)

採点済み結果の詳細を取得する。レスポンス形状は[POST_attempts-id-submit.md](./POST_attempts-id-submit.md)のレスポンスと同一（`correct_answer_snapshot`から復元）。

```json
{
  "attemptId": "att-01",
  "rawScore": 7,
  "maxScore": 10,
  "answers": [
    {
      "questionId": "q1",
      "userAnswerText": "TRUE",
      "isCorrect": true,
      "correctAnswer": "TRUE",
      "explanation": "Paragraph B states that green spaces lower surrounding temperatures."
    }
  ]
}
```

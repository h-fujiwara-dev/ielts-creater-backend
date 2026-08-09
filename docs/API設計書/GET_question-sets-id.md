# GET /api/v1/question-sets/{id}

- 関連文書: [API一覧](../API一覧.md) / [POST_question-sets.md](./POST_question-sets.md) / [ER図・テーブル定義](../ER図・テーブル定義.md)

問題セット詳細を取得する（正解は含めない）。

レスポンス（200 OK）:
```json
{
  "id": "b3f1...",
  "section": "READING",
  "status": "READY",
  "passage": {
    "title": "The Impact of Urban Green Spaces",
    "paragraphs": [{ "id": "A", "text": "..." }]
  },
  "questionGroups": [
    {
      "formatType": "TFNG",
      "instructions": "Do the following statements agree with the information in the passage?",
      "questions": [
        { "id": "q1", "promptText": "Urban parks reduce average city temperatures.", "displayOrder": 1 }
      ]
    }
  ]
}
```
正解（`correctAnswerKey`）・解説は含めない。`status`が`GENERATING`の間はフロントエンドがポーリングする（[POST_question-sets.md 生成フロー](./POST_question-sets.md#生成フロー)を参照）。

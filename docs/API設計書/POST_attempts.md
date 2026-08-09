# POST /api/v1/attempts

- 関連文書: [API一覧](../API一覧.md) / [PATCH_attempts-id-answers.md](./PATCH_attempts-id-answers.md) / [ER図・テーブル定義 attempt](../ER図・テーブル定義.md#310-attempt)

受験（Attempt）を開始する。

```json
// request
{ "questionSetId": "b3f1..." }
// response 201
{ "id": "att-01", "status": "IN_PROGRESS" }
```

# GET /api/v1/attempts/{id}/answers

- 関連文書: [API一覧](../API一覧.md) / [PATCH_attempts-id-answers.md](./PATCH_attempts-id-answers.md) / [ER図・テーブル定義 attempt_answer](../ER図・テーブル定義.md#311-attempt_answer)

保存済みの回答一覧を取得する。回答画面（S-04）のページ再読み込み後に、直前まで入力していた回答を復元するために使用する（システム要件定義書F-07）。

採点情報（`isCorrect` / `correctAnswer` / `explanation`）は含めない。採点済みの結果を取得する場合は[GET_attempts-id.md](./GET_attempts-id.md)を使用する。

レスポンス（200 OK）:

```json
{
  "attemptId": "att-01",
  "status": "IN_PROGRESS",
  "answers": [
    { "questionId": "q1", "userAnswerText": "TRUE" }
  ]
}
```

- `answers`には保存済み（`PATCH /api/v1/attempts/{id}/answers`で送信済み）の設問のみを含む。未回答の設問は含めない
- `status`が`SUBMITTED`のAttemptに対しても呼び出し可能（提出時点の最終回答テキストをそのまま返す）が、通常フロントエンドは`IN_PROGRESS`時の復元用途でのみ呼び出す
- 他ユーザーのAttemptを指定した場合、または存在しないIDの場合は404（`ResourceNotFoundException`）

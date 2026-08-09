# GET /api/v1/dashboard/summary

- 関連文書: [API一覧](../API一覧.md) / [ER図・テーブル定義](../ER図・テーブル定義.md)

ダッシュボード集計データを取得する。`DashboardSummaryService`が受験履歴からスコア推移・正答率を集計する。

```json
{
  "totalAttempts": 42,
  "averageAccuracyBySection": { "READING": 0.74, "LISTENING": 0.68 },
  "scoreTrend": [
    { "date": "2026-08-01", "accuracy": 0.6 },
    { "date": "2026-08-07", "accuracy": 0.75 }
  ],
  "accuracyByFormat": { "TFNG": 0.8, "MCQ": 0.7, "FILL_BLANK": 0.6, "MATCHING_HEADINGS": 0.65 }
}
```

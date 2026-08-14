# GET /api/v1/dashboard/summary

- 関連文書: [API一覧](../API一覧.md) / [ER図・テーブル定義](../ER図・テーブル定義.md)

ダッシュボード集計データを取得する。`DashboardSummaryService`が受験履歴からスコア推移・正答率を集計する。集計対象は`GET /api/v1/attempts`と同様、`status=SUBMITTED`のAttemptのみ。

## クエリパラメータ

| パラメータ | 必須 | デフォルト | 説明 |
| --- | --- | --- | --- |
| `period` | 任意 | `ALL` | `7D` / `30D` / `90D` / `ALL`。`attempt.submitted_at`が現在時刻からこの期間内のものに絞り込む |
| `section` | 任意 | 指定なし（両セクション） | `READING` / `LISTENING`。指定時は当該セクションのみ集計 |

サーバー側（DBクエリ）で絞り込んだ上で集計する。`section`を指定した場合、`averageAccuracyBySection`・`accuracyByFormat`は指定セクションのみの値を返す。

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

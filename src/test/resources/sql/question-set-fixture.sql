-- questionset系結合テスト専用のフィクスチャ。app_userのIDのみNoAuthCurrentUserProvider.DEV_USER_IDと一致させる。
-- 各テストメソッド実行前に毎回適用される（Spring @Sqlのデフォルト実行タイミング）ため、このスイート自身が
-- 生成したquestion_set（prompt_version='stub-v1'で識別）を都度クリーンアップし、各テストメソッドが
-- 「その日の生成回数0件」から独立して開始できるようにする。他の結合テストクラス（例: attempt-fixture.sql、
-- prompt_version='test-v1'）のデータには触れない。
--
-- 対象IDの特定〜全テーブルの削除を単一のCTEチェーン文（1トランザクション・1ステートメント）にまとめている。
-- 複数の独立したDELETE文に分けると、READ COMMITTEDでは文ごとに新しいスナップショットを取り直すため、
-- 別の結合テストクラス実行中に並行して走る非同期生成ワーカーがDELETE文の合間にコミットしてしまい、
-- 後続のDELETEが「削除直前に出現した」子行の外部キー制約に失敗することがある（1文にすることで対象IDを
-- 最初に一度だけ確定させ、この競合を構造的に防ぐ）。

INSERT INTO app_user (id, cognito_sub, email, display_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'dev-user', 'dev@ielts-creator.local', 'Dev User')
ON CONFLICT (id) DO NOTHING;

WITH target_question_sets AS (
  SELECT id FROM question_set WHERE prompt_version = 'stub-v1'
),
deleted_audio_segments AS (
  DELETE FROM audio_segment
  WHERE listening_script_id IN (
    SELECT id FROM listening_script WHERE question_set_id IN (SELECT id FROM target_question_sets)
  )
  RETURNING id
),
deleted_listening_scripts AS (
  DELETE FROM listening_script
  WHERE question_set_id IN (SELECT id FROM target_question_sets)
  RETURNING id
),
deleted_acceptable_answers AS (
  DELETE FROM acceptable_answer
  WHERE question_id IN (
    SELECT q.id FROM question q
    JOIN question_group qg ON q.question_group_id = qg.id
    WHERE qg.question_set_id IN (SELECT id FROM target_question_sets)
  )
  RETURNING id
),
deleted_answer_options AS (
  DELETE FROM answer_option
  WHERE question_id IN (
    SELECT q.id FROM question q
    JOIN question_group qg ON q.question_group_id = qg.id
    WHERE qg.question_set_id IN (SELECT id FROM target_question_sets)
  )
  RETURNING id
),
deleted_questions AS (
  DELETE FROM question
  WHERE question_group_id IN (
    SELECT id FROM question_group WHERE question_set_id IN (SELECT id FROM target_question_sets)
  )
  RETURNING id
),
deleted_question_groups AS (
  DELETE FROM question_group
  WHERE question_set_id IN (SELECT id FROM target_question_sets)
  RETURNING id
),
deleted_passages AS (
  DELETE FROM passage
  WHERE question_set_id IN (SELECT id FROM target_question_sets)
  RETURNING id
)
DELETE FROM question_set WHERE id IN (SELECT id FROM target_question_sets);

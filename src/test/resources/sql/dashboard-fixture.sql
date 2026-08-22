-- dashboard集計API結合テスト専用のフィクスチャ。attempt系のfixture(attempt-fixture.sql)とは独立させ、
-- 採点フローは経由せずAttempt/AttemptAnswerの最終状態を直接投入する。
-- submitted_atはnow()からの相対値にして、period(7D/30D/90D)フィルタのテストが実行時刻に依存せず安定するようにする。

INSERT INTO app_user (id, cognito_sub, email, display_name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'dev-user', 'dev@ielts-creator.local', 'Dev User'),
    ('00000000-0000-0000-0000-000000000002', 'other-user', 'other@ielts-creator.local', 'Other User')
ON CONFLICT (id) DO NOTHING;

-- created_atを明示的に過去日付にし、QuestionSetGenerationService.checkDailyLimit（同一dev-userの
-- 当日生成件数カウント）にこのフィクスチャ行が算入されないようにする。他の結合テストクラス
-- （QuestionSetApiIntegrationTest）が同一JVM内で後続実行される際に、日次上限を消費してしまうのを防ぐ。
INSERT INTO question_set (id, user_id, section, topic, difficulty, status, prompt_version, created_at) VALUES
    ('30000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'READING', 'Dashboard fixture reading', 'BAND_6_7', 'READY', 'test-v1', now() - interval '2 days'),
    ('30000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'LISTENING', 'Dashboard fixture listening', 'BAND_6_7', 'READY', 'test-v1', now() - interval '2 days'),
    ('30000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 'READING', 'Other user reading', 'BAND_6_7', 'READY', 'test-v1', now() - interval '2 days');

-- Reading: TFNG / MCQ / FILL_BLANK
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order) VALUES
    ('30000000-0000-0000-0000-000000000101', '30000000-0000-0000-0000-000000000001', 'TFNG', 'TFNG instructions', 1),
    ('30000000-0000-0000-0000-000000000102', '30000000-0000-0000-0000-000000000001', 'MCQ', 'MCQ instructions', 2),
    ('30000000-0000-0000-0000-000000000103', '30000000-0000-0000-0000-000000000001', 'FILL_BLANK', 'FILL_BLANK instructions', 3);

-- Listening: FORM_COMPLETION
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order) VALUES
    ('30000000-0000-0000-0000-000000000104', '30000000-0000-0000-0000-000000000002', 'FORM_COMPLETION', 'FORM_COMPLETION instructions', 1);

-- Other user's reading question (used only to prove user isolation)
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order) VALUES
    ('30000000-0000-0000-0000-000000000105', '30000000-0000-0000-0000-000000000003', 'TFNG', 'Other user TFNG', 1);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key) VALUES
    ('30000000-0000-0000-0000-000000001001', '30000000-0000-0000-0000-000000000101', 'TFNG prompt 1', 1, '"TRUE"'),
    ('30000000-0000-0000-0000-000000001002', '30000000-0000-0000-0000-000000000101', 'TFNG prompt 2', 2, '"FALSE"'),
    ('30000000-0000-0000-0000-000000001003', '30000000-0000-0000-0000-000000000102', 'MCQ prompt', 1, '["B"]'),
    ('30000000-0000-0000-0000-000000001004', '30000000-0000-0000-0000-000000000103', 'FILL_BLANK prompt', 1, '"carbon dioxide"'),
    ('30000000-0000-0000-0000-000000001005', '30000000-0000-0000-0000-000000000104', 'FORM_COMPLETION prompt', 1, '"answer"'),
    ('30000000-0000-0000-0000-000000001006', '30000000-0000-0000-0000-000000000105', 'Other user TFNG prompt', 1, '"TRUE"');

-- Attempt A: dev user, READING, submitted 1 day ago, 1/2 correct (TFNG only)
INSERT INTO attempt (id, user_id, question_set_id, status, submitted_at, raw_score, max_score) VALUES
    ('30000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'SUBMITTED', now() - interval '1 day', 1, 2);

-- Attempt B: dev user, READING, submitted 40 days ago (outside 30D window), 3/3 correct (TFNG+MCQ+FILL_BLANK)
INSERT INTO attempt (id, user_id, question_set_id, status, submitted_at, raw_score, max_score) VALUES
    ('30000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'SUBMITTED', now() - interval '40 days', 3, 3);

-- Attempt C: dev user, LISTENING, submitted 2 days ago, 1/1 correct (FORM_COMPLETION)
INSERT INTO attempt (id, user_id, question_set_id, status, submitted_at, raw_score, max_score) VALUES
    ('30000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 'SUBMITTED', now() - interval '2 days', 1, 1);

-- Attempt D: dev user, READING, still IN_PROGRESS (must never appear in the aggregation)
INSERT INTO attempt (id, user_id, question_set_id, status) VALUES
    ('30000000-0000-0000-0000-000000002004', '00000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'IN_PROGRESS');

-- Attempt E: other user, READING, submitted 1 day ago (must never appear in the dev user's dashboard)
INSERT INTO attempt (id, user_id, question_set_id, status, submitted_at, raw_score, max_score) VALUES
    ('30000000-0000-0000-0000-000000002005', '00000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000003', 'SUBMITTED', now() - interval '1 day', 1, 1);

INSERT INTO attempt_answer (attempt_id, question_id, user_answer_text, is_correct, correct_answer_snapshot) VALUES
    ('30000000-0000-0000-0000-000000002001', '30000000-0000-0000-0000-000000001001', 'TRUE', true, '"TRUE"'),
    ('30000000-0000-0000-0000-000000002001', '30000000-0000-0000-0000-000000001002', 'TRUE', false, '"FALSE"'),
    ('30000000-0000-0000-0000-000000002002', '30000000-0000-0000-0000-000000001001', 'TRUE', true, '"TRUE"'),
    ('30000000-0000-0000-0000-000000002002', '30000000-0000-0000-0000-000000001003', 'B', true, '["B"]'),
    ('30000000-0000-0000-0000-000000002002', '30000000-0000-0000-0000-000000001004', 'CO2', true, '"carbon dioxide"'),
    ('30000000-0000-0000-0000-000000002003', '30000000-0000-0000-0000-000000001005', 'answer', true, '"answer"'),
    ('30000000-0000-0000-0000-000000002005', '30000000-0000-0000-0000-000000001006', 'TRUE', true, '"TRUE"');

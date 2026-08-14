-- attempt系結合テスト専用のフィクスチャ。db/dev-migrationのシードとは完全に独立させ、
-- app_userのIDのみNoAuthCurrentUserProvider.DEV_USER_IDと一致させる。

INSERT INTO app_user (id, cognito_sub, email, display_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'dev-user', 'dev@ielts-creator.local', 'Dev User')
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_set (id, user_id, section, topic, difficulty, status, prompt_version)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'READING',
    'Integration test fixture',
    'BAND_6_7',
    'READY',
    'test-v1'
);

-- TFNG
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '20000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000001',
    'TFNG',
    'TFNG instructions',
    1
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key, explanation)
VALUES (
    '20000000-0000-0000-0000-000000001001',
    '20000000-0000-0000-0000-000000000101',
    'TFNG prompt',
    1,
    '"TRUE"',
    'tfng explanation'
);

-- MCQ
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '20000000-0000-0000-0000-000000000102',
    '20000000-0000-0000-0000-000000000001',
    'MCQ',
    'MCQ instructions',
    2
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key, explanation)
VALUES (
    '20000000-0000-0000-0000-000000001002',
    '20000000-0000-0000-0000-000000000102',
    'MCQ prompt',
    1,
    '["B"]',
    'mcq explanation'
);

INSERT INTO answer_option (question_id, option_label, option_text) VALUES
    ('20000000-0000-0000-0000-000000001002', 'A', 'Option A'),
    ('20000000-0000-0000-0000-000000001002', 'B', 'Option B');

-- FILL_BLANK
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '20000000-0000-0000-0000-000000000103',
    '20000000-0000-0000-0000-000000000001',
    'FILL_BLANK',
    'FILL_BLANK instructions',
    3
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key, explanation)
VALUES (
    '20000000-0000-0000-0000-000000001003',
    '20000000-0000-0000-0000-000000000103',
    'FILL_BLANK prompt',
    1,
    '"carbon dioxide"',
    'fill blank explanation'
);

INSERT INTO acceptable_answer (question_id, answer_text, normalized_text) VALUES
    ('20000000-0000-0000-0000-000000001003', 'carbon dioxide', 'carbon dioxide'),
    ('20000000-0000-0000-0000-000000001003', 'CO2', 'co2');

-- MATCHING_HEADINGS
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '20000000-0000-0000-0000-000000000104',
    '20000000-0000-0000-0000-000000000001',
    'MATCHING_HEADINGS',
    'MATCHING_HEADINGS instructions',
    4
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, metadata, correct_answer_key, explanation)
VALUES (
    '20000000-0000-0000-0000-000000001004',
    '20000000-0000-0000-0000-000000000104',
    'MATCHING_HEADINGS prompt',
    1,
    '{"paragraphRef":"A"}',
    '"iv"',
    'matching headings explanation'
);

INSERT INTO answer_option (question_id, option_label, option_text) VALUES
    ('20000000-0000-0000-0000-000000001004', 'iii', 'Wrong heading'),
    ('20000000-0000-0000-0000-000000001004', 'iv', 'Correct heading');

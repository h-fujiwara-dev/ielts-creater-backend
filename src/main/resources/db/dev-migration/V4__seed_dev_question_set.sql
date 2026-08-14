-- ローカル開発(no-auth)専用のシードデータ。問題生成API未実装のため、attempt系APIの手動確認用に
-- 主要4出題形式(TFNG/MCQ/FILL_BLANK/MATCHING_HEADINGS)を含むサンプルquestion_setを投入する。

INSERT INTO question_set (id, user_id, section, topic, difficulty, status, prompt_version)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'READING',
    'Rainforest conservation (seed data for attempt API manual testing)',
    'BAND_6_7',
    'READY',
    'seed-v1'
);

-- TFNG
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '10000000-0000-0000-0000-000000000101',
    '10000000-0000-0000-0000-000000000001',
    'TFNG',
    'Do the following statements agree with the information given? Write TRUE, FALSE, or NOT GIVEN.',
    1
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key, explanation)
VALUES (
    '10000000-0000-0000-0000-000000001001',
    '10000000-0000-0000-0000-000000000101',
    'The Amazon rainforest produces roughly 20% of the world''s oxygen.',
    1,
    '"TRUE"',
    'The passage states this fact directly in paragraph A.'
);

-- MCQ
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '10000000-0000-0000-0000-000000000102',
    '10000000-0000-0000-0000-000000000001',
    'MCQ',
    'Choose the correct letter, A, B, C or D.',
    2
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key, explanation)
VALUES (
    '10000000-0000-0000-0000-000000001002',
    '10000000-0000-0000-0000-000000000102',
    'What is the main cause of rainforest loss mentioned in the passage?',
    1,
    '["B"]',
    'Paragraph B identifies agricultural expansion as the primary driver.'
);

INSERT INTO answer_option (question_id, option_label, option_text) VALUES
    ('10000000-0000-0000-0000-000000001002', 'A', 'Climate change'),
    ('10000000-0000-0000-0000-000000001002', 'B', 'Agricultural expansion'),
    ('10000000-0000-0000-0000-000000001002', 'C', 'Urban development'),
    ('10000000-0000-0000-0000-000000001002', 'D', 'Mining operations');

-- FILL_BLANK
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '10000000-0000-0000-0000-000000000103',
    '10000000-0000-0000-0000-000000000001',
    'FILL_BLANK',
    'Complete the summary below using NO MORE THAN TWO WORDS.',
    3
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, correct_answer_key, explanation)
VALUES (
    '10000000-0000-0000-0000-000000001003',
    '10000000-0000-0000-0000-000000000103',
    'The rainforest absorbs large amounts of ______ from the atmosphere.',
    1,
    '"carbon dioxide"',
    'See paragraph C for details on carbon absorption.'
);

INSERT INTO acceptable_answer (question_id, answer_text, normalized_text) VALUES
    ('10000000-0000-0000-0000-000000001003', 'carbon dioxide', 'carbon dioxide'),
    ('10000000-0000-0000-0000-000000001003', 'CO2', 'co2');

-- MATCHING_HEADINGS
INSERT INTO question_group (id, question_set_id, format_type, instructions, display_order)
VALUES (
    '10000000-0000-0000-0000-000000000104',
    '10000000-0000-0000-0000-000000000001',
    'MATCHING_HEADINGS',
    'Match each paragraph with the correct heading from the list below.',
    4
);

INSERT INTO question (id, question_group_id, prompt_text, display_order, metadata, correct_answer_key, explanation)
VALUES (
    '10000000-0000-0000-0000-000000001004',
    '10000000-0000-0000-0000-000000000104',
    'Paragraph A',
    1,
    '{"paragraphRef":"A"}',
    '"iv"',
    'Paragraph A introduces the rainforest ecosystem in general terms.'
);

INSERT INTO answer_option (question_id, option_label, option_text) VALUES
    ('10000000-0000-0000-0000-000000001004', 'i', 'The economic impact of deforestation'),
    ('10000000-0000-0000-0000-000000001004', 'ii', 'Wildlife under threat'),
    ('10000000-0000-0000-0000-000000001004', 'iii', 'International conservation efforts'),
    ('10000000-0000-0000-0000-000000001004', 'iv', 'An overview of the rainforest ecosystem');

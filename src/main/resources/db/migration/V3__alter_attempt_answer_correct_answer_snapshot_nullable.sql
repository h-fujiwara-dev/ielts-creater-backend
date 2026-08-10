-- PATCH /attempts/{id}/answers による部分保存時点ではまだ採点されておらず、
-- correct_answer_snapshotは提出(POST /attempts/{id}/submit)時にのみ確定するためNULLを許容する。
ALTER TABLE attempt_answer ALTER COLUMN correct_answer_snapshot DROP NOT NULL;

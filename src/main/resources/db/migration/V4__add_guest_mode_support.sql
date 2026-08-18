-- ゲスト機能（#00056）: 共有デモアカウント（Cognitoのclient_idクレームで判定）かどうかのフラグ
ALTER TABLE app_user ADD COLUMN is_guest BOOLEAN NOT NULL DEFAULT false;

-- GuestDataCleanupServiceがquestion_setを起点に一括削除できるよう、子テーブルへON DELETE CASCADEを
-- 付与する。通常ユーザーのquestion_set/attemptはこのバッチの削除対象にならないため実質的な影響はない。
ALTER TABLE passage
    DROP CONSTRAINT passage_question_set_id_fkey,
    ADD CONSTRAINT passage_question_set_id_fkey
        FOREIGN KEY (question_set_id) REFERENCES question_set (id) ON DELETE CASCADE;

ALTER TABLE listening_script
    DROP CONSTRAINT listening_script_question_set_id_fkey,
    ADD CONSTRAINT listening_script_question_set_id_fkey
        FOREIGN KEY (question_set_id) REFERENCES question_set (id) ON DELETE CASCADE;

ALTER TABLE audio_segment
    DROP CONSTRAINT audio_segment_listening_script_id_fkey,
    ADD CONSTRAINT audio_segment_listening_script_id_fkey
        FOREIGN KEY (listening_script_id) REFERENCES listening_script (id) ON DELETE CASCADE;

ALTER TABLE question_group
    DROP CONSTRAINT question_group_question_set_id_fkey,
    ADD CONSTRAINT question_group_question_set_id_fkey
        FOREIGN KEY (question_set_id) REFERENCES question_set (id) ON DELETE CASCADE;

ALTER TABLE question
    DROP CONSTRAINT question_question_group_id_fkey,
    ADD CONSTRAINT question_question_group_id_fkey
        FOREIGN KEY (question_group_id) REFERENCES question_group (id) ON DELETE CASCADE;

ALTER TABLE answer_option
    DROP CONSTRAINT answer_option_question_id_fkey,
    ADD CONSTRAINT answer_option_question_id_fkey
        FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE;

ALTER TABLE acceptable_answer
    DROP CONSTRAINT acceptable_answer_question_id_fkey,
    ADD CONSTRAINT acceptable_answer_question_id_fkey
        FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE;

ALTER TABLE attempt
    DROP CONSTRAINT attempt_question_set_id_fkey,
    ADD CONSTRAINT attempt_question_set_id_fkey
        FOREIGN KEY (question_set_id) REFERENCES question_set (id) ON DELETE CASCADE;

ALTER TABLE attempt_answer
    DROP CONSTRAINT attempt_answer_attempt_id_fkey,
    ADD CONSTRAINT attempt_answer_attempt_id_fkey
        FOREIGN KEY (attempt_id) REFERENCES attempt (id) ON DELETE CASCADE;

ALTER TABLE attempt_answer
    DROP CONSTRAINT attempt_answer_question_id_fkey,
    ADD CONSTRAINT attempt_answer_question_id_fkey
        FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE;

-- ゲストの問題生成（POST /api/v1/question-sets）に対するIPアドレス単位の日次クォータカウンタ。
-- ユーザーID単位の既存日次上限（QuestionSetGenerationService）は共有デモアカウントに対しては
-- バイパスされ、代わりにこちらがGuestQuotaInterceptorから原子的にINSERT ... ON CONFLICTで参照・更新される。
CREATE TABLE guest_ip_quota (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ip_address     VARCHAR(45) NOT NULL,
    usage_date     DATE        NOT NULL,
    request_count  INT         NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (ip_address, usage_date)
);

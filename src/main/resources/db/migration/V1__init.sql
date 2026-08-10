CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub   VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE question_set (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES app_user (id),
    section           VARCHAR(16)  NOT NULL,
    topic             VARCHAR(200) NOT NULL,
    difficulty        VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    generation_error  TEXT,
    prompt_version    VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_question_set_user ON question_set (user_id, created_at DESC);

CREATE TABLE passage (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_set_id  UUID         NOT NULL REFERENCES question_set (id),
    title            VARCHAR(255),
    body_json        JSONB        NOT NULL
);

CREATE TABLE listening_script (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_set_id  UUID         NOT NULL REFERENCES question_set (id),
    context_text     VARCHAR(500),
    script_json      JSONB        NOT NULL
);

CREATE TABLE audio_segment (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listening_script_id   UUID         NOT NULL REFERENCES listening_script (id),
    turn_index            INT          NOT NULL,
    s3_key                VARCHAR(500) NOT NULL,
    duration_ms           INT,
    voice_id              VARCHAR(50)  NOT NULL
);

CREATE TABLE question_group (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_set_id  UUID         NOT NULL REFERENCES question_set (id),
    format_type      VARCHAR(30)  NOT NULL,
    instructions     TEXT         NOT NULL,
    display_order    INT          NOT NULL
);

CREATE TABLE question (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_group_id    UUID  NOT NULL REFERENCES question_group (id),
    prompt_text          TEXT  NOT NULL,
    display_order        INT   NOT NULL,
    metadata             JSONB,
    correct_answer_key   JSONB NOT NULL,
    explanation          TEXT
);

CREATE TABLE answer_option (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id    UUID        NOT NULL REFERENCES question (id),
    option_label   VARCHAR(5)  NOT NULL,
    option_text    TEXT        NOT NULL
);

CREATE TABLE acceptable_answer (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id       UUID         NOT NULL REFERENCES question (id),
    answer_text       VARCHAR(200) NOT NULL,
    normalized_text   VARCHAR(200) NOT NULL
);

CREATE TABLE attempt (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES app_user (id),
    question_set_id  UUID        NOT NULL REFERENCES question_set (id),
    status           VARCHAR(20) NOT NULL,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at     TIMESTAMPTZ,
    raw_score        INT,
    max_score        INT
);

CREATE INDEX idx_attempt_user_created ON attempt (user_id, submitted_at DESC);

CREATE TABLE attempt_answer (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id                UUID    NOT NULL REFERENCES attempt (id),
    question_id               UUID    NOT NULL REFERENCES question (id),
    user_answer_text          TEXT,
    is_correct                BOOLEAN,
    correct_answer_snapshot   JSONB   NOT NULL
);

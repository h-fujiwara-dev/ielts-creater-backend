-- ローカル開発(no-auth)専用のシードデータ。application-local.ymlのみでこのlocationを読み込む。
INSERT INTO app_user (id, cognito_sub, email, display_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'dev-user', 'dev@ielts-creator.local', 'Dev User')
ON CONFLICT (id) DO NOTHING;

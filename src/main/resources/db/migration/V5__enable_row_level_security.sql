-- 本番Supabase Security Advisorが検出した「RLS Disabled in Public」対応（#00071）。
-- アプリはpostgres.<project-ref>（Supabaseのpostgresスーパーユーザー、RLSを常にバイパス）で
-- JDBC直接接続しているため、ここでRLSを有効化してもアプリの動作には影響しない。
-- SupabaseはデフォルトでpublicスキーマをPostgREST経由でも公開するため、RLS未設定のままだと
-- 外部から直接テーブルを読み書きされうるリスクがある。ポリシーは追加せず有効化のみ行う。
--
-- flyway_schema_historyはRLS対象から除外している。このテーブルにRLSを有効化すると、
-- Flyway自身のマイグレーション後処理（doRestoreOriginalStateでのスキーマ存在確認）と
-- 競合し、アプリが起動しなくなることをローカル検証で確認済み。同テーブルはマイグレーション
-- 履歴のみを保持し実データを含まないためリスクは軽微と判断し、対象から外す。
ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_set ENABLE ROW LEVEL SECURITY;
ALTER TABLE passage ENABLE ROW LEVEL SECURITY;
ALTER TABLE listening_script ENABLE ROW LEVEL SECURITY;
ALTER TABLE audio_segment ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_group ENABLE ROW LEVEL SECURITY;
ALTER TABLE question ENABLE ROW LEVEL SECURITY;
ALTER TABLE answer_option ENABLE ROW LEVEL SECURITY;
ALTER TABLE acceptable_answer ENABLE ROW LEVEL SECURITY;
ALTER TABLE attempt ENABLE ROW LEVEL SECURITY;
ALTER TABLE attempt_answer ENABLE ROW LEVEL SECURITY;
ALTER TABLE guest_ip_quota ENABLE ROW LEVEL SECURITY;

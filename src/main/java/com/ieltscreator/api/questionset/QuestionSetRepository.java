package com.ieltscreator.api.questionset;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, UUID> {

  long countByUserIdAndCreatedAtBetween(UUID userId, Instant createdAtFrom, Instant createdAtTo);

  /**
   * ユーザー単位のPostgresトランザクションスコープアドバイザリロック。同時リクエストによる
   * 日次生成回数チェック（count）→INSERTのcheck-then-actレースを、呼び出し元の
   * {@code @Transactional}メソッド内で直列化して防ぐ（#00064）。トランザクション終了時に自動解放される。
   */
  @Query(
      value = "SELECT pg_advisory_xact_lock(hashtext(CAST(:userId AS text)))",
      nativeQuery = true)
  void acquireDailyLimitLock(@Param("userId") UUID userId);

  /**
   * ゲスト（#00056）の共有デモアカウントに紐づくquestion_setのうちcutoffより古いものを検索する。 question_set/attempt等の子テーブルにはON
   * DELETE CASCADEを付与済みのため（V4migration）、 ここで見つけた行をdeleteするだけで受験履歴・採点結果まで含めて一括削除できる
   * （attemptはquestion_setより後にしか作られないため、attempt単独での古さ判定は不要）。
   */
  @Query(
      """
      SELECT qs FROM QuestionSet qs, com.ieltscreator.api.user.AppUser u
      WHERE u.id = qs.userId AND u.isGuest = true AND qs.createdAt < :cutoff
      """)
  List<QuestionSet> findStaleGuestQuestionSets(@Param("cutoff") Instant cutoff);
}

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

package com.ieltscreator.api.dashboard;

import com.ieltscreator.api.dashboard.dto.AttemptScoreRow;
import com.ieltscreator.api.dashboard.dto.FormatAccuracyRow;
import com.ieltscreator.api.questionset.Section;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** attempt/questionsetを横断する、ダッシュボード集計専用の読み取りクエリ（実装規約.md 2.1）。 */
@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {

  private static final String ATTEMPT_SCORE_QUERY =
      """
      select new com.ieltscreator.api.dashboard.dto.AttemptScoreRow(
          qs.section, a.submittedAt, a.rawScore, a.maxScore)
      from Attempt a
      join QuestionSet qs on qs.id = a.questionSetId
      where a.userId = :userId
        and a.status = com.ieltscreator.api.attempt.AttemptStatus.SUBMITTED
        and (:section is null or qs.section = :section)
        and a.submittedAt >= :submittedAfter
      """;

  private static final String FORMAT_ACCURACY_QUERY =
      """
      select new com.ieltscreator.api.dashboard.dto.FormatAccuracyRow(
          qg.formatType, aa.isCorrect)
      from AttemptAnswer aa
      join Attempt a on a.id = aa.attemptId
      join QuestionSet qs on qs.id = a.questionSetId
      join Question q on q.id = aa.questionId
      join q.questionGroup qg
      where a.userId = :userId
        and a.status = com.ieltscreator.api.attempt.AttemptStatus.SUBMITTED
        and aa.isCorrect is not null
        and (:section is null or qs.section = :section)
        and a.submittedAt >= :submittedAfter
      """;

  private final EntityManager entityManager;

  public List<AttemptScoreRow> findAttemptScores(
      UUID userId, Section section, Instant submittedAfter) {
    return entityManager
        .createQuery(ATTEMPT_SCORE_QUERY, AttemptScoreRow.class)
        .setParameter("userId", userId)
        .setParameter("section", section)
        .setParameter("submittedAfter", submittedAfter)
        .getResultList();
  }

  public List<FormatAccuracyRow> findFormatAccuracy(
      UUID userId, Section section, Instant submittedAfter) {
    return entityManager
        .createQuery(FORMAT_ACCURACY_QUERY, FormatAccuracyRow.class)
        .setParameter("userId", userId)
        .setParameter("section", section)
        .setParameter("submittedAfter", submittedAfter)
        .getResultList();
  }
}

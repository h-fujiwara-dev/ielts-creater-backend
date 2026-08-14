package com.ieltscreator.api.attempt;

import com.ieltscreator.api.attempt.dto.AttemptHistoryItemResponse;
import com.ieltscreator.api.questionset.Section;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

  @Query(
      """
      select new com.ieltscreator.api.attempt.dto.AttemptHistoryItemResponse(
          a.id, a.questionSetId, qs.section, a.submittedAt, a.rawScore, a.maxScore)
      from Attempt a
      join QuestionSet qs on qs.id = a.questionSetId
      where a.userId = :userId
        and a.status = com.ieltscreator.api.attempt.AttemptStatus.SUBMITTED
        and (:section is null or qs.section = :section)
      order by a.submittedAt desc
      """)
  Page<AttemptHistoryItemResponse> findHistory(
      @Param("userId") UUID userId, @Param("section") Section section, Pageable pageable);
}

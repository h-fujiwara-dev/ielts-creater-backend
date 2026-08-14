package com.ieltscreator.api.questionset;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

  @EntityGraph(attributePaths = "questionGroup")
  List<Question> findAllByQuestionGroup_QuestionSetId(UUID questionSetId);
}

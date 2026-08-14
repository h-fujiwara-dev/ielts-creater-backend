package com.ieltscreator.api.questionset;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, UUID> {

  List<QuestionGroup> findAllByQuestionSetIdOrderByDisplayOrderAsc(UUID questionSetId);
}

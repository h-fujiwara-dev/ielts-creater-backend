package com.ieltscreator.api.questionset;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, UUID> {

  List<AnswerOption> findAllByQuestionIdIn(List<UUID> questionIds);
}

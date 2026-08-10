package com.ieltscreator.api.attempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {

  List<AttemptAnswer> findAllByAttemptId(UUID attemptId);

  Optional<AttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}

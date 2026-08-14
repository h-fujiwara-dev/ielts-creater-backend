package com.ieltscreator.api.questionset;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptableAnswerRepository extends JpaRepository<AcceptableAnswer, UUID> {

  List<AcceptableAnswer> findAllByQuestionId(UUID questionId);
}

package com.ieltscreator.api.questionset;

import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** questionSetIdの存在確認と所有者チェックを一箇所にまとめる（他ユーザーのQuestionSetは404）。 */
@Component
@RequiredArgsConstructor
class QuestionSetFinder {

  private final QuestionSetRepository questionSetRepository;

  QuestionSet findOwned(UUID userId, UUID questionSetId) {
    return questionSetRepository
        .findById(questionSetId)
        .filter(questionSet -> questionSet.getUserId().equals(userId))
        .orElseThrow(
            () -> new ResourceNotFoundException("QuestionSet not found: " + questionSetId));
  }
}

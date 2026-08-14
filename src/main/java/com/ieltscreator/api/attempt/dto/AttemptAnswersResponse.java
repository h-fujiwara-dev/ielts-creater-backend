package com.ieltscreator.api.attempt.dto;

import com.ieltscreator.api.attempt.AttemptStatus;
import java.util.List;
import java.util.UUID;

public record AttemptAnswersResponse(
    UUID attemptId, AttemptStatus status, List<SavedAnswer> answers) {

  public record SavedAnswer(UUID questionId, String userAnswerText) {}
}

package com.ieltscreator.api.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AttemptAnswerSaveRequest(@NotEmpty @Valid List<AnswerItem> answers) {

  public record AnswerItem(@NotNull UUID questionId, String userAnswerText) {}
}

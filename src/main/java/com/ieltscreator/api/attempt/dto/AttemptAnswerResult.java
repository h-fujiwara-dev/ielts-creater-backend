package com.ieltscreator.api.attempt.dto;

import java.util.UUID;

public record AttemptAnswerResult(
    UUID questionId,
    String userAnswerText,
    Boolean isCorrect,
    String correctAnswer,
    String explanation) {}

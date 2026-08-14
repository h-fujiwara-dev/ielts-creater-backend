package com.ieltscreator.api.attempt.dto;

import java.util.List;
import java.util.UUID;

public record AttemptResultResponse(
    UUID attemptId, Integer rawScore, Integer maxScore, List<AttemptAnswerResult> answers) {}

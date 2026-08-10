package com.ieltscreator.api.attempt.dto;

import com.ieltscreator.api.questionset.Section;
import java.time.Instant;
import java.util.UUID;

public record AttemptHistoryItemResponse(
    UUID attemptId,
    UUID questionSetId,
    Section section,
    Instant submittedAt,
    Integer rawScore,
    Integer maxScore) {}

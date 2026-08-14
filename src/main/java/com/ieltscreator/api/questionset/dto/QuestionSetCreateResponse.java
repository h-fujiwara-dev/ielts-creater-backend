package com.ieltscreator.api.questionset.dto;

import com.ieltscreator.api.questionset.QuestionSetStatus;
import java.util.UUID;

public record QuestionSetCreateResponse(UUID id, QuestionSetStatus status, String topic) {}

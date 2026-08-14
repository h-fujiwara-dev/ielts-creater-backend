package com.ieltscreator.api.questionset.dto;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
    UUID id, String promptText, Integer displayOrder, List<AnswerOptionResponse> answerOptions) {}

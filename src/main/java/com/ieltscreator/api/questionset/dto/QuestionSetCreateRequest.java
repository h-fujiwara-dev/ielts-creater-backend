package com.ieltscreator.api.questionset.dto;

import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.Section;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionSetCreateRequest(
    @NotNull Section section, @Size(max = 100) String topic, @NotNull Difficulty difficulty) {}

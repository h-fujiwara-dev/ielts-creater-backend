package com.ieltscreator.api.questionset.generation;

import com.ieltscreator.api.questionset.QuestionFormatType;
import java.util.List;

public record GeneratedQuestionGroup(
    QuestionFormatType formatType, String instructions, List<GeneratedQuestion> questions) {}

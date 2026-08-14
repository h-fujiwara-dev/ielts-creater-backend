package com.ieltscreator.api.questionset.generation;

import java.util.List;

public record GeneratedReadingContent(
    GeneratedPassage passage, List<GeneratedQuestionGroup> questionGroups) {}

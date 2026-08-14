package com.ieltscreator.api.questionset.generation;

import java.util.List;

public record GeneratedListeningContent(
    GeneratedListeningScript script, List<GeneratedQuestionGroup> questionGroups) {}

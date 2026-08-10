package com.ieltscreator.api.questionset.generation;

import java.util.List;

public record GeneratedListeningScript(
    String contextText, List<GeneratedSpeaker> speakers, List<GeneratedTurn> turns) {}

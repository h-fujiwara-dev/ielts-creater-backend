package com.ieltscreator.api.questionset.generation;

import java.util.List;

/** {@code listening-question-schema.json}にJSON形状を一致させたOpenAIレスポンスのパース先。 */
record ListeningSchemaDto(
    String scriptContextText,
    List<TurnDto> turns,
    FillBlankQuestionDto formCompletionQuestion1,
    FillBlankQuestionDto formCompletionQuestion2,
    FillBlankQuestionDto noteCompletionQuestion1,
    FillBlankQuestionDto noteCompletionQuestion2) {

  record TurnDto(String speaker, String text) {}

  record FillBlankQuestionDto(
      String promptText,
      String primaryAnswer,
      List<String> acceptableAnswers,
      String explanation) {}
}

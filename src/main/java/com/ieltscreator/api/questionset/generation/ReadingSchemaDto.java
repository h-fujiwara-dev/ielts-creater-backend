package com.ieltscreator.api.questionset.generation;

import java.util.List;

/** {@code reading-question-schema.json}にJSON形状を一致させたOpenAIレスポンスのパース先。 */
record ReadingSchemaDto(
    String passageTitle,
    String paragraphA,
    String paragraphB,
    String paragraphC,
    String paragraphD,
    TfngQuestionDto tfngStatement1,
    TfngQuestionDto tfngStatement2,
    McqQuestionDto mcqQuestion,
    FillBlankQuestionDto fillBlankQuestion,
    String headingOptionI,
    String headingOptionII,
    String headingOptionIII,
    String headingOptionIV,
    MatchingHeadingQuestionDto matchingHeadingA,
    MatchingHeadingQuestionDto matchingHeadingB,
    MatchingHeadingQuestionDto matchingHeadingC,
    MatchingHeadingQuestionDto matchingHeadingD) {

  record TfngQuestionDto(String promptText, String correctAnswer, String explanation) {}

  record McqQuestionDto(
      String promptText,
      String optionA,
      String optionB,
      String optionC,
      String optionD,
      String correctLabel,
      String explanation) {}

  record FillBlankQuestionDto(
      String promptText,
      String primaryAnswer,
      List<String> acceptableAnswers,
      String explanation) {}

  record MatchingHeadingQuestionDto(String correctHeadingLabel, String explanation) {}
}

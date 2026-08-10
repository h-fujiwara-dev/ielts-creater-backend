package com.ieltscreator.api.questionset.generation;

import java.util.List;
import java.util.Map;

/**
 * 生成された1設問。正解の表現はformatTypeにより異なる: TFNG/MATCHING_HEADINGSは{@code correctAnswerText}、MCQは{@code
 * correctAnswerLabels}、FILL_BLANK系は{@code correctAnswerText} ＋{@code acceptableAnswers}を用いる。
 */
public record GeneratedQuestion(
    String promptText,
    Map<String, Object> metadata,
    String correctAnswerText,
    List<String> correctAnswerLabels,
    List<GeneratedAnswerOption> answerOptions,
    List<String> acceptableAnswers,
    String explanation) {

  public static GeneratedQuestion tfng(
      String promptText, String correctAnswer, String explanation) {
    return new GeneratedQuestion(
        promptText, Map.of(), correctAnswer, List.of(), List.of(), List.of(), explanation);
  }

  public static GeneratedQuestion mcq(
      String promptText,
      List<GeneratedAnswerOption> options,
      List<String> correctLabels,
      String explanation) {
    return new GeneratedQuestion(
        promptText, Map.of(), null, correctLabels, options, List.of(), explanation);
  }

  public static GeneratedQuestion matchingHeading(
      String promptText,
      String paragraphRef,
      List<GeneratedAnswerOption> headingOptions,
      String correctLabel,
      String explanation) {
    return new GeneratedQuestion(
        promptText,
        Map.of("paragraphRef", paragraphRef),
        correctLabel,
        List.of(),
        headingOptions,
        List.of(),
        explanation);
  }

  public static GeneratedQuestion fillBlank(
      String promptText,
      int maxWords,
      String primaryAnswer,
      List<String> acceptableAnswers,
      String explanation) {
    return new GeneratedQuestion(
        promptText,
        Map.of("maxWords", maxWords),
        primaryAnswer,
        List.of(),
        List.of(),
        acceptableAnswers,
        explanation);
  }
}

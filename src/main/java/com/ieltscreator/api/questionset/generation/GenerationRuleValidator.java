package com.ieltscreator.api.questionset.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * JSONスキーマ検証に加えて行う内容面のルールベース検証。違反があれば理由の一覧を返す（空なら合格）。 AI生成が方式に沿ったJSONを返してきても、内容として矛盾している場合（例:
 * 見出しラベルが選択肢に存在しない、語数制限を超過している）を検出する。
 *
 * <p>加えて、生成された自由記述テキスト（設問文・選択肢・解説・パッセージ本文等）に対する安全性チェック（HTMLタグらしき文字列・長さ上限）も ここで行う。OpenAI Structured
 * Outputs（strict mode）はJSON Schemaの{@code maxLength}等の文字列制約キーワードを
 * サポートしないため、Schema側ではなくここでの検証が唯一の強制ポイントになる（#00064）。
 */
@Component
public class GenerationRuleValidator {

  private static final Set<String> TFNG_VALUES = Set.of("TRUE", "FALSE", "NOT GIVEN");
  private static final String BLANK_MARKER = "______";
  private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
  private static final int MAX_FREE_TEXT_LENGTH = 1000;

  public List<String> validate(List<GeneratedQuestionGroup> questionGroups) {
    List<String> violations = new ArrayList<>();
    for (GeneratedQuestionGroup group : questionGroups) {
      for (GeneratedQuestion question : group.questions()) {
        switch (group.formatType()) {
          case TFNG -> validateTfng(question, violations);
          case MCQ -> validateMcq(question, violations);
          case MATCHING_HEADINGS -> validateMatchingHeadings(question, violations);
          case FILL_BLANK, FORM_COMPLETION, NOTE_COMPLETION ->
              validateFillBlank(question, violations);
        }
        validateQuestionFreeTextSafety(question, violations);
      }
    }
    return violations;
  }

  /** パッセージ本文・Listening台本等、設問グループの外にある自由記述テキストの安全性チェック。 */
  public List<String> validateFreeText(List<String> texts) {
    List<String> violations = new ArrayList<>();
    for (String text : texts) {
      checkFreeTextSafety("content", text, violations);
    }
    return violations;
  }

  private void validateQuestionFreeTextSafety(GeneratedQuestion question, List<String> violations) {
    checkFreeTextSafety("promptText", question.promptText(), violations);
    checkFreeTextSafety("explanation", question.explanation(), violations);
    for (GeneratedAnswerOption option : question.answerOptions()) {
      checkFreeTextSafety("optionText", option.text(), violations);
    }
    for (String answer : question.acceptableAnswers()) {
      checkFreeTextSafety("acceptableAnswer", answer, violations);
    }
  }

  private static void checkFreeTextSafety(String fieldLabel, String text, List<String> violations) {
    if (text == null) {
      return;
    }
    if (HTML_TAG_PATTERN.matcher(text).find()) {
      violations.add("%s contains HTML-like tag syntax".formatted(fieldLabel));
    }
    if (text.length() > MAX_FREE_TEXT_LENGTH) {
      violations.add(
          "%s exceeds max length %d (actual %d)"
              .formatted(fieldLabel, MAX_FREE_TEXT_LENGTH, text.length()));
    }
  }

  private void validateTfng(GeneratedQuestion question, List<String> violations) {
    if (question.correctAnswerText() == null
        || !TFNG_VALUES.contains(question.correctAnswerText())) {
      violations.add(
          "TFNG question has invalid correctAnswerText \"%s\" (question: %s)"
              .formatted(question.correctAnswerText(), question.promptText()));
    }
  }

  private void validateMcq(GeneratedQuestion question, List<String> violations) {
    if (question.correctAnswerLabels() == null || question.correctAnswerLabels().isEmpty()) {
      violations.add(
          "MCQ question has no correctAnswerLabels (question: %s)"
              .formatted(question.promptText()));
      return;
    }
    Set<String> optionLabels = labelsOf(question);
    for (String label : question.correctAnswerLabels()) {
      if (!optionLabels.contains(label)) {
        violations.add(
            "MCQ correctAnswerLabel \"%s\" not found in answerOptions (question: %s)"
                .formatted(label, question.promptText()));
      }
    }
  }

  private void validateMatchingHeadings(GeneratedQuestion question, List<String> violations) {
    Set<String> headingLabels = labelsOf(question);
    if (question.correctAnswerText() == null
        || !headingLabels.contains(question.correctAnswerText())) {
      violations.add(
          "MATCHING_HEADINGS correctHeadingLabel \"%s\" not found in headingOptions (question: %s)"
              .formatted(question.correctAnswerText(), question.promptText()));
    }
  }

  private void validateFillBlank(GeneratedQuestion question, List<String> violations) {
    int blankCount = countOccurrences(question.promptText(), BLANK_MARKER);
    if (blankCount != 1) {
      violations.add(
          "Fill-in-the-blank question must contain exactly one \"%s\" marker but found %d (question: %s)"
              .formatted(BLANK_MARKER, blankCount, question.promptText()));
    }
    if (question.acceptableAnswers() == null || question.acceptableAnswers().isEmpty()) {
      violations.add(
          "Fill-in-the-blank question has no acceptableAnswers (question: %s)"
              .formatted(question.promptText()));
      return;
    }
    Integer maxWords = extractMaxWords(question);
    if (maxWords == null) {
      return;
    }
    for (String answer : question.acceptableAnswers()) {
      if (wordCount(answer) > maxWords) {
        violations.add(
            "Acceptable answer \"%s\" exceeds maxWords=%d (question: %s)"
                .formatted(answer, maxWords, question.promptText()));
      }
    }
  }

  private static Set<String> labelsOf(GeneratedQuestion question) {
    return question.answerOptions().stream()
        .map(GeneratedAnswerOption::label)
        .collect(Collectors.toSet());
  }

  private static Integer extractMaxWords(GeneratedQuestion question) {
    Object value = question.metadata() == null ? null : question.metadata().get("maxWords");
    return value instanceof Number number ? number.intValue() : null;
  }

  private static int wordCount(String text) {
    String trimmed = text == null ? "" : text.strip();
    return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
  }

  private static int countOccurrences(String text, String marker) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(marker, index)) != -1) {
      count++;
      index += marker.length();
    }
    return count;
  }
}

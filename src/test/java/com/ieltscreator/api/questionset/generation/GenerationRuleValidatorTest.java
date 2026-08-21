package com.ieltscreator.api.questionset.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.questionset.QuestionFormatType;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenerationRuleValidatorTest {

  private final GenerationRuleValidator validator = new GenerationRuleValidator();

  private static final List<GeneratedAnswerOption> MCQ_OPTIONS =
      List.of(
          new GeneratedAnswerOption("A", "Option A"), new GeneratedAnswerOption("B", "Option B"));
  private static final List<GeneratedAnswerOption> HEADING_OPTIONS =
      List.of(
          new GeneratedAnswerOption("i", "Heading i"),
          new GeneratedAnswerOption("ii", "Heading ii"));

  @Test
  void returnsNoViolationsForValidContent() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.TFNG,
                "instructions",
                List.of(GeneratedQuestion.tfng("Q1", "TRUE", null))),
            new GeneratedQuestionGroup(
                QuestionFormatType.MCQ,
                "instructions",
                List.of(GeneratedQuestion.mcq("Q2", MCQ_OPTIONS, List.of("B"), null))),
            new GeneratedQuestionGroup(
                QuestionFormatType.MATCHING_HEADINGS,
                "instructions",
                List.of(GeneratedQuestion.matchingHeading("Q3", "A", HEADING_OPTIONS, "ii", null))),
            new GeneratedQuestionGroup(
                QuestionFormatType.FILL_BLANK,
                "instructions",
                List.of(
                    GeneratedQuestion.fillBlank(
                        "Q4 is the ______.", 2, "answer", List.of("answer", "the answer"), null))));

    assertThat(validator.validate(groups)).isEmpty();
  }

  @Test
  void flagsInvalidTfngAnswer() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.TFNG,
                "instructions",
                List.of(GeneratedQuestion.tfng("Q1", "MAYBE", null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("TFNG"));
  }

  @Test
  void flagsMcqCorrectLabelNotInOptions() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.MCQ,
                "instructions",
                List.of(GeneratedQuestion.mcq("Q2", MCQ_OPTIONS, List.of("Z"), null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("MCQ"));
  }

  @Test
  void flagsMatchingHeadingsLabelNotInHeadingOptions() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.MATCHING_HEADINGS,
                "instructions",
                List.of(GeneratedQuestion.matchingHeading("Q3", "A", HEADING_OPTIONS, "v", null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("MATCHING_HEADINGS"));
  }

  @Test
  void flagsAcceptableAnswerExceedingMaxWords() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.FILL_BLANK,
                "instructions",
                List.of(
                    GeneratedQuestion.fillBlank(
                        "Q4 is the ______.", 2, "one two three", List.of("one two three"), null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("maxWords"));
  }

  @Test
  void flagsFillBlankQuestionWithNoAcceptableAnswers() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.NOTE_COMPLETION,
                "instructions",
                List.of(
                    GeneratedQuestion.fillBlank(
                        "Q5 is the ______.", 2, "answer", List.of(), null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("acceptableAnswers"));
  }

  @Test
  void flagsFillBlankQuestionWithoutBlankMarker() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.FILL_BLANK,
                "instructions",
                List.of(
                    GeneratedQuestion.fillBlank(
                        "Q6 has no marker.", 2, "answer", List.of("answer"), null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("______"));
  }

  @Test
  void flagsFillBlankQuestionWithMultipleBlankMarkers() {
    List<GeneratedQuestionGroup> groups =
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.FORM_COMPLETION,
                "instructions",
                List.of(
                    GeneratedQuestion.fillBlank(
                        "Name: ______ Phone: ______", 2, "answer", List.of("answer"), null))));

    assertThat(validator.validate(groups)).anyMatch(v -> v.contains("______"));
  }
}

package com.ieltscreator.api.questionset.generation;

import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.QuestionFormatType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OpenAI連携の実装に差し替えるまでの決定的なReading生成器。topic/difficultyを内容に反映しつつ、
 * TFNG/MCQ/FILL_BLANK/MATCHING_HEADINGSの4形式を{@link GenerationRuleValidator}が必ず通る形で生成する。
 */
@Component
@ConditionalOnProperty(
    prefix = "app.generation",
    name = "mode",
    havingValue = "stub",
    matchIfMissing = true)
public class StubReadingQuestionGenerator implements ReadingQuestionGenerator {

  @Override
  public GeneratedReadingContent generate(String topic, Difficulty difficulty) {
    GeneratedPassage passage = buildPassage(topic);
    List<GeneratedQuestionGroup> questionGroups =
        List.of(
            buildTfngGroup(topic),
            buildMcqGroup(topic),
            buildFillBlankGroup(topic, difficulty),
            buildMatchingHeadingsGroup());
    return new GeneratedReadingContent(passage, questionGroups);
  }

  private GeneratedPassage buildPassage(String topic) {
    List<GeneratedParagraph> paragraphs =
        List.of(
            new GeneratedParagraph(
                "A",
                "This passage introduces %s and outlines why it has become an important subject "
                    .concat(
                        "of discussion in recent years. Researchers estimate that %s affects daily life ")
                    .concat("for millions of people worldwide.")
                    .formatted(topic, topic)),
            new GeneratedParagraph(
                "B",
                "One of the main challenges related to %s is balancing economic growth with "
                    .concat(
                        "sustainable practices. Governments and organisations have proposed a range of ")
                    .concat("policies to address the issue.")
                    .formatted(topic)),
            new GeneratedParagraph(
                "C",
                "Despite ongoing efforts, %s continues to raise concerns among experts. Data "
                    .concat(
                        "collected over the past decade suggests that current approaches absorb ")
                    .concat("significant amounts of public and private investment.")
                    .formatted(topic)),
            new GeneratedParagraph(
                "D",
                "Looking ahead, most specialists agree that solving problems tied to %s will require "
                    .concat(
                        "international cooperation. Continued study of %s remains essential for ")
                    .concat("building resilient systems in the future.")
                    .formatted(topic, topic)));
    return new GeneratedPassage("%s: An Overview".formatted(topic), paragraphs);
  }

  private GeneratedQuestionGroup buildTfngGroup(String topic) {
    List<GeneratedQuestion> questions =
        List.of(
            GeneratedQuestion.tfng(
                "The passage states that %s affects only a small number of people."
                    .formatted(topic),
                "FALSE",
                "Paragraph A says the issue affects millions of people worldwide."),
            GeneratedQuestion.tfng(
                "The writer suggests that international cooperation may be necessary to address this"
                    + " issue.",
                "TRUE",
                "Paragraph D states that solving the problem will require international"
                    + " cooperation."));
    return new GeneratedQuestionGroup(
        QuestionFormatType.TFNG,
        "Do the following statements agree with the information given? Write TRUE, FALSE, or NOT"
            + " GIVEN.",
        questions);
  }

  private GeneratedQuestionGroup buildMcqGroup(String topic) {
    List<GeneratedAnswerOption> options =
        List.of(
            new GeneratedAnswerOption("A", "A lack of public awareness"),
            new GeneratedAnswerOption("B", "Balancing economic growth with sustainable practices"),
            new GeneratedAnswerOption("C", "Insufficient scientific research"),
            new GeneratedAnswerOption("D", "Excessive government regulation"));
    GeneratedQuestion question =
        GeneratedQuestion.mcq(
            "According to the passage, what is one of the main challenges related to %s?"
                .formatted(topic),
            options,
            List.of("B"),
            "Paragraph B identifies balancing economic growth with sustainability as a key"
                + " challenge.");
    return new GeneratedQuestionGroup(
        QuestionFormatType.MCQ, "Choose the correct letter, A, B, C or D.", List.of(question));
  }

  private GeneratedQuestionGroup buildFillBlankGroup(String topic, Difficulty difficulty) {
    int maxWords = leniencyMaxWords(difficulty);
    GeneratedQuestion question =
        GeneratedQuestion.fillBlank(
            "Continued study of %s remains essential for building ______ systems in the future."
                .formatted(topic),
            maxWords,
            "resilient",
            List.of("resilient", "resilient systems"),
            "See paragraph D for the concluding remark on future systems.");
    return new GeneratedQuestionGroup(
        QuestionFormatType.FILL_BLANK,
        "Complete the sentence below using NO MORE THAN %d WORDS.".formatted(maxWords),
        List.of(question));
  }

  private GeneratedQuestionGroup buildMatchingHeadingsGroup() {
    List<GeneratedAnswerOption> headingOptions =
        List.of(
            new GeneratedAnswerOption("i", "International cooperation as the way forward"),
            new GeneratedAnswerOption("ii", "The economic and sustainability trade-off"),
            new GeneratedAnswerOption("iii", "An introduction to a widespread issue"),
            new GeneratedAnswerOption("iv", "Ongoing concerns despite continued efforts"));
    List<GeneratedQuestion> questions =
        List.of(
            GeneratedQuestion.matchingHeading(
                "Paragraph A", "A", headingOptions, "iii", "Paragraph A introduces the issue."),
            GeneratedQuestion.matchingHeading(
                "Paragraph B",
                "B",
                headingOptions,
                "ii",
                "Paragraph B discusses the economic and sustainability trade-off."),
            GeneratedQuestion.matchingHeading(
                "Paragraph C",
                "C",
                headingOptions,
                "iv",
                "Paragraph C describes ongoing concerns despite continued efforts."),
            GeneratedQuestion.matchingHeading(
                "Paragraph D",
                "D",
                headingOptions,
                "i",
                "Paragraph D calls for international cooperation."));
    return new GeneratedQuestionGroup(
        QuestionFormatType.MATCHING_HEADINGS,
        "Match each paragraph with the correct heading from the list below.",
        questions);
  }

  private static int leniencyMaxWords(Difficulty difficulty) {
    return switch (difficulty) {
      case BAND_4_5, BAND_5_6 -> 3;
      case BAND_6_7, BAND_7_8_PLUS -> 2;
    };
  }
}

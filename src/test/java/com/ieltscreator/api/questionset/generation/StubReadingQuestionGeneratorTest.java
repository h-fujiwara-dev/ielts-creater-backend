package com.ieltscreator.api.questionset.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.QuestionFormatType;
import java.util.List;
import org.junit.jupiter.api.Test;

class StubReadingQuestionGeneratorTest {

  private final StubReadingQuestionGenerator generator = new StubReadingQuestionGenerator();
  private final GenerationRuleValidator validator = new GenerationRuleValidator();

  @Test
  void generatesAllFourFormatTypesAndEmbedsTopic() {
    GeneratedReadingContent content = generator.generate("Renewable energy", Difficulty.BAND_6_7);

    assertThat(content.passage().title()).contains("Renewable energy");
    assertThat(content.passage().paragraphs()).hasSize(4);
    assertThat(content.questionGroups())
        .extracting(GeneratedQuestionGroup::formatType)
        .containsExactly(
            QuestionFormatType.TFNG,
            QuestionFormatType.MCQ,
            QuestionFormatType.FILL_BLANK,
            QuestionFormatType.MATCHING_HEADINGS);
  }

  @Test
  void generatedContentPassesRuleValidation() {
    for (Difficulty difficulty : Difficulty.values()) {
      GeneratedReadingContent content = generator.generate("Urban planning", difficulty);
      assertThat(validator.validate(content.questionGroups())).isEmpty();
    }
  }

  @Test
  void matchingHeadingsHasOneQuestionPerParagraph() {
    GeneratedReadingContent content = generator.generate("Ocean conservation", Difficulty.BAND_4_5);

    GeneratedQuestionGroup matchingHeadings =
        content.questionGroups().stream()
            .filter(g -> g.formatType() == QuestionFormatType.MATCHING_HEADINGS)
            .findFirst()
            .orElseThrow();

    List<String> paragraphIds =
        content.passage().paragraphs().stream().map(GeneratedParagraph::id).toList();
    assertThat(matchingHeadings.questions()).hasSameSizeAs(paragraphIds);
  }
}

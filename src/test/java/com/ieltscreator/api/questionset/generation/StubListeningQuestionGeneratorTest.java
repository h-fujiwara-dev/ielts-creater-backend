package com.ieltscreator.api.questionset.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.QuestionFormatType;
import org.junit.jupiter.api.Test;

class StubListeningQuestionGeneratorTest {

  private final StubListeningQuestionGenerator generator = new StubListeningQuestionGenerator();
  private final GenerationRuleValidator validator = new GenerationRuleValidator();

  @Test
  void generatesFormAndNoteCompletionGroupsAndEmbedsTopic() {
    GeneratedListeningContent content = generator.generate("Marine biology", Difficulty.BAND_6_7);

    assertThat(content.script().contextText()).contains("Marine biology");
    assertThat(content.script().speakers()).hasSize(2);
    assertThat(content.script().turns()).isNotEmpty();
    // frontendが未対応のFORM_COMPLETION/NOTE_COMPLETIONではなく、frontend対応済みの
    // FILL_BLANKとして生成する（#00054）。
    assertThat(content.questionGroups())
        .extracting(GeneratedQuestionGroup::formatType)
        .containsExactly(QuestionFormatType.FILL_BLANK, QuestionFormatType.FILL_BLANK);
  }

  @Test
  void everyTurnReferencesAKnownSpeaker() {
    GeneratedListeningContent content =
        generator.generate("City planning", Difficulty.BAND_7_8_PLUS);

    var speakerIds = content.script().speakers().stream().map(GeneratedSpeaker::id).toList();
    assertThat(content.script().turns())
        .extracting(GeneratedTurn::speakerId)
        .allMatch(speakerIds::contains);
  }

  @Test
  void generatedContentPassesRuleValidation() {
    for (Difficulty difficulty : Difficulty.values()) {
      GeneratedListeningContent content = generator.generate("Public transport", difficulty);
      assertThat(validator.validate(content.questionGroups())).isEmpty();
    }
  }
}

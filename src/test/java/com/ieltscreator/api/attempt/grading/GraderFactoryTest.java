package com.ieltscreator.api.attempt.grading;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.questionset.QuestionFormatType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraderFactoryTest {

  @Mock private TfngGrader tfngGrader;
  @Mock private McqGrader mcqGrader;
  @Mock private FillBlankGrader fillBlankGrader;
  @Mock private MatchingHeadingsGrader matchingHeadingsGrader;

  private GraderFactory factory() {
    return new GraderFactory(tfngGrader, mcqGrader, fillBlankGrader, matchingHeadingsGrader);
  }

  @Test
  void resolvesTfngGraderForTfng() {
    assertThat(factory().resolve(QuestionFormatType.TFNG)).isEqualTo(tfngGrader);
  }

  @Test
  void resolvesMcqGraderForMcq() {
    assertThat(factory().resolve(QuestionFormatType.MCQ)).isEqualTo(mcqGrader);
  }

  @Test
  void resolvesMatchingHeadingsGraderForMatchingHeadings() {
    assertThat(factory().resolve(QuestionFormatType.MATCHING_HEADINGS))
        .isEqualTo(matchingHeadingsGrader);
  }

  /**
   * FORM_COMPLETION/NOTE_COMPLETIONをFillBlankGraderへ統一したのは#00054の不具合修正
   * （未対応フォーマットで生成されListening問題に回答できなかった）そのものであり、 ここでの回帰を防ぐことが特に重要。
   */
  @Test
  void resolvesFillBlankGraderForFillBlankFormCompletionAndNoteCompletion() {
    assertThat(factory().resolve(QuestionFormatType.FILL_BLANK)).isEqualTo(fillBlankGrader);
    assertThat(factory().resolve(QuestionFormatType.FORM_COMPLETION)).isEqualTo(fillBlankGrader);
    assertThat(factory().resolve(QuestionFormatType.NOTE_COMPLETION)).isEqualTo(fillBlankGrader);
  }
}

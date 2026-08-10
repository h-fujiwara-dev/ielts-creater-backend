package com.ieltscreator.api.attempt.grading;

import com.ieltscreator.api.questionset.QuestionFormatType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GraderFactory {

  private final TfngGrader tfngGrader;
  private final McqGrader mcqGrader;
  private final FillBlankGrader fillBlankGrader;
  private final MatchingHeadingsGrader matchingHeadingsGrader;

  public AnswerGrader resolve(QuestionFormatType formatType) {
    return switch (formatType) {
      case TFNG -> tfngGrader;
      case MCQ -> mcqGrader;
      case FILL_BLANK, FORM_COMPLETION, NOTE_COMPLETION -> fillBlankGrader;
      case MATCHING_HEADINGS -> matchingHeadingsGrader;
    };
  }
}

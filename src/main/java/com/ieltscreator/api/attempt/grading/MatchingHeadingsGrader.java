package com.ieltscreator.api.attempt.grading;

import com.ieltscreator.api.questionset.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingHeadingsGrader implements AnswerGrader {

  private final AnswerKeyCodec answerKeyCodec;

  @Override
  public boolean isCorrect(Question question, String userAnswerText) {
    if (userAnswerText == null) {
      return false;
    }
    String correct = answerKeyCodec.asPlainString(question.getCorrectAnswerKey());
    return userAnswerText.equals(correct);
  }
}

package com.ieltscreator.api.attempt.grading;

import com.ieltscreator.api.questionset.AcceptableAnswer;
import com.ieltscreator.api.questionset.AcceptableAnswerRepository;
import com.ieltscreator.api.questionset.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** FILL_BLANK / FORM_COMPLETION / NOTE_COMPLETION共通の採点。acceptable_answerとの正規化後一致で判定する。 */
@Component
@RequiredArgsConstructor
public class FillBlankGrader implements AnswerGrader {

  private final AcceptableAnswerRepository acceptableAnswerRepository;
  private final AnswerNormalizer answerNormalizer;

  @Override
  public boolean isCorrect(Question question, String userAnswerText) {
    if (userAnswerText == null || userAnswerText.isBlank()) {
      return false;
    }
    String normalized = answerNormalizer.normalize(userAnswerText);
    return acceptableAnswerRepository.findAllByQuestionId(question.getId()).stream()
        .map(AcceptableAnswer::getNormalizedText)
        .anyMatch(normalized::equals);
  }
}

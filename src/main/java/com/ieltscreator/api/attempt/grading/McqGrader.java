package com.ieltscreator.api.attempt.grading;

import com.ieltscreator.api.questionset.Question;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 選択ラベル集合がcorrect_answer_keyの集合と完全一致するかを判定する（部分点なし）。 */
@Component
@RequiredArgsConstructor
public class McqGrader implements AnswerGrader {

  private final AnswerKeyCodec answerKeyCodec;

  @Override
  public boolean isCorrect(Question question, String userAnswerText) {
    if (userAnswerText == null || userAnswerText.isBlank()) {
      return false;
    }
    Set<String> submitted =
        Arrays.stream(userAnswerText.split(","))
            .map(String::strip)
            .filter(label -> !label.isEmpty())
            .collect(Collectors.toSet());
    Set<String> correct =
        new HashSet<>(answerKeyCodec.asStringList(question.getCorrectAnswerKey()));
    return submitted.equals(correct);
  }
}

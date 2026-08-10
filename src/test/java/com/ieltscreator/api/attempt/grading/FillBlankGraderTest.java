package com.ieltscreator.api.attempt.grading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.text.AnswerNormalizer;
import com.ieltscreator.api.questionset.AcceptableAnswer;
import com.ieltscreator.api.questionset.AcceptableAnswerRepository;
import com.ieltscreator.api.questionset.Question;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FillBlankGraderTest {

  @Mock private AcceptableAnswerRepository acceptableAnswerRepository;

  private final AnswerNormalizer answerNormalizer = new AnswerNormalizer();

  private FillBlankGrader grader() {
    return new FillBlankGrader(acceptableAnswerRepository, answerNormalizer);
  }

  @Test
  void returnsTrueWhenNormalizedAnswerMatchesAnAcceptableAnswer() {
    UUID questionId = UUID.randomUUID();
    Question question = Question.builder().id(questionId).build();
    when(acceptableAnswerRepository.findAllByQuestionId(questionId))
        .thenReturn(
            List.of(
                acceptableAnswer("carbon dioxide", "carbon dioxide"),
                acceptableAnswer("CO2", "co2")));

    assertThat(grader().isCorrect(question, "CO2.")).isTrue();
  }

  @Test
  void returnsFalseWhenNoAcceptableAnswerMatches() {
    UUID questionId = UUID.randomUUID();
    Question question = Question.builder().id(questionId).build();
    when(acceptableAnswerRepository.findAllByQuestionId(questionId))
        .thenReturn(List.of(acceptableAnswer("carbon dioxide", "carbon dioxide")));

    assertThat(grader().isCorrect(question, "oxygen")).isFalse();
  }

  @Test
  void returnsFalseForBlankAnswer() {
    Question question = Question.builder().id(UUID.randomUUID()).build();

    assertThat(grader().isCorrect(question, "")).isFalse();
    assertThat(grader().isCorrect(question, null)).isFalse();
  }

  private static AcceptableAnswer acceptableAnswer(String answerText, String normalizedText) {
    return AcceptableAnswer.builder().answerText(answerText).normalizedText(normalizedText).build();
  }
}

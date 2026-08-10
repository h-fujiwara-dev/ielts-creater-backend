package com.ieltscreator.api.attempt.grading;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.questionset.Question;
import org.junit.jupiter.api.Test;

class TfngGraderTest {

  private final TfngGrader grader = new TfngGrader(new AnswerKeyCodec(new ObjectMapper()));

  @Test
  void returnsTrueWhenAnswerMatchesExactly() {
    Question question = Question.builder().correctAnswerKey("\"TRUE\"").build();

    assertThat(grader.isCorrect(question, "TRUE")).isTrue();
  }

  @Test
  void returnsFalseWhenAnswerDiffers() {
    Question question = Question.builder().correctAnswerKey("\"TRUE\"").build();

    assertThat(grader.isCorrect(question, "FALSE")).isFalse();
  }

  @Test
  void returnsFalseWhenAnswerIsNull() {
    Question question = Question.builder().correctAnswerKey("\"TRUE\"").build();

    assertThat(grader.isCorrect(question, null)).isFalse();
  }
}

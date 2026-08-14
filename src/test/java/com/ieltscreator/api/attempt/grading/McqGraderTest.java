package com.ieltscreator.api.attempt.grading;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.questionset.Question;
import org.junit.jupiter.api.Test;

class McqGraderTest {

  private final McqGrader grader = new McqGrader(new AnswerKeyCodec(new ObjectMapper()));

  @Test
  void returnsTrueForSingleLabelMatch() {
    Question question = Question.builder().correctAnswerKey("[\"B\"]").build();

    assertThat(grader.isCorrect(question, "B")).isTrue();
  }

  @Test
  void returnsTrueForMultiLabelMatchRegardlessOfOrderAndSpacing() {
    Question question = Question.builder().correctAnswerKey("[\"A\",\"C\"]").build();

    assertThat(grader.isCorrect(question, "C, A")).isTrue();
  }

  @Test
  void returnsFalseForPartialMatch() {
    Question question = Question.builder().correctAnswerKey("[\"A\",\"C\"]").build();

    assertThat(grader.isCorrect(question, "A")).isFalse();
  }

  @Test
  void returnsFalseForBlankAnswer() {
    Question question = Question.builder().correctAnswerKey("[\"A\"]").build();

    assertThat(grader.isCorrect(question, "")).isFalse();
    assertThat(grader.isCorrect(question, null)).isFalse();
  }
}

package com.ieltscreator.api.attempt.grading;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.questionset.Question;
import org.junit.jupiter.api.Test;

class MatchingHeadingsGraderTest {

  private final MatchingHeadingsGrader grader =
      new MatchingHeadingsGrader(new AnswerKeyCodec(new ObjectMapper()));

  @Test
  void returnsTrueWhenLabelMatchesExactly() {
    Question question = Question.builder().correctAnswerKey("\"iv\"").build();

    assertThat(grader.isCorrect(question, "iv")).isTrue();
  }

  @Test
  void returnsFalseWhenLabelDiffers() {
    Question question = Question.builder().correctAnswerKey("\"iv\"").build();

    assertThat(grader.isCorrect(question, "ii")).isFalse();
  }

  @Test
  void returnsFalseWhenAnswerIsNull() {
    Question question = Question.builder().correctAnswerKey("\"iv\"").build();

    assertThat(grader.isCorrect(question, null)).isFalse();
  }
}

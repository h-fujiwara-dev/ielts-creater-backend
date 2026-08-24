package com.ieltscreator.api.attempt.grading;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AnswerKeyCodecTest {

  private final AnswerKeyCodec codec = new AnswerKeyCodec(new ObjectMapper());

  @Test
  void asPlainStringReadsJsonScalarString() {
    assertThat(codec.asPlainString("\"answer\"")).isEqualTo("answer");
  }

  @Test
  void asStringListReadsJsonArray() {
    assertThat(codec.asStringList("[\"A\",\"C\"]")).containsExactly("A", "C");
  }

  @Test
  void asStringListReturnsEmptyListForEmptyArray() {
    assertThat(codec.asStringList("[]")).isEmpty();
  }

  @Test
  void asDisplayTextJoinsArrayWithCommaSpace() {
    assertThat(codec.asDisplayText("[\"A\",\"C\"]")).isEqualTo("A, C");
  }

  @Test
  void asDisplayTextReturnsPlainStringAsIs() {
    assertThat(codec.asDisplayText("\"answer\"")).isEqualTo("answer");
  }
}

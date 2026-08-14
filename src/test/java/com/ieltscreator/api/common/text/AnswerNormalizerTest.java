package com.ieltscreator.api.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnswerNormalizerTest {

  private final AnswerNormalizer normalizer = new AnswerNormalizer();

  @Test
  void trimsAndLowercases() {
    assertThat(normalizer.normalize("  Carbon Dioxide  ")).isEqualTo("carbon dioxide");
  }

  @Test
  void collapsesInternalWhitespace() {
    assertThat(normalizer.normalize("carbon   dioxide")).isEqualTo("carbon dioxide");
  }

  @Test
  void stripsTrailingPunctuation() {
    assertThat(normalizer.normalize("CO2.")).isEqualTo("co2");
  }

  @Test
  void returnsEmptyStringForNull() {
    assertThat(normalizer.normalize(null)).isEmpty();
  }
}

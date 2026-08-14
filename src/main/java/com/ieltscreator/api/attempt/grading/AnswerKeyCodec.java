package com.ieltscreator.api.attempt.grading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

/** question.correctAnswerKey / attemptAnswer.correctAnswerSnapshot（JSON文字列）の読み書きを行う。 */
@Component
@RequiredArgsConstructor
public class AnswerKeyCodec {

  private final ObjectMapper objectMapper;

  @SneakyThrows
  public String asPlainString(String json) {
    return objectMapper.readTree(json).asText();
  }

  @SneakyThrows
  public List<String> asStringList(String json) {
    JsonNode node = objectMapper.readTree(json);
    List<String> values = new ArrayList<>();
    node.forEach(item -> values.add(item.asText()));
    return values;
  }

  /** 画面表示用のテキスト表現。JSON配列はカンマ区切りにする。 */
  @SneakyThrows
  public String asDisplayText(String json) {
    JsonNode node = objectMapper.readTree(json);
    if (node.isArray()) {
      return String.join(", ", asStringList(json));
    }
    return node.asText();
  }
}

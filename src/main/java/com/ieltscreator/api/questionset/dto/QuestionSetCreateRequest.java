package com.ieltscreator.api.questionset.dto;

import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.Section;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionSetCreateRequest(
    @NotNull Section section, @Size(max = 100) String topic, @NotNull Difficulty difficulty) {

  public QuestionSetCreateRequest {
    topic = normalizeTopic(topic);
  }

  // 制御文字・改行をプロンプトインジェクション対策のデリミタ崩し等に使われないよう単一の空白へ正規化する
  // （文字種そのものは制限しない。過度な制限は自然なトピック入力を壊すため、#00064）。
  private static String normalizeTopic(String topic) {
    if (topic == null) {
      return null;
    }
    return topic.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip();
  }
}

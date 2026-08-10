package com.ieltscreator.api.questionset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audio_segment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AudioSegment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listening_script_id", nullable = false)
  private ListeningScript listeningScript;

  @Column(name = "turn_index", nullable = false)
  private Integer turnIndex;

  /** Phase1はローカル保存ファイルの相対パス、Phase3ではS3オブジェクトキーを格納する。 */
  @Column(name = "s3_key", nullable = false, length = 500)
  private String s3Key;

  @Column(name = "duration_ms")
  private Integer durationMs;

  @Column(name = "voice_id", nullable = false, length = 50)
  private String voiceId;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AudioSegment other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

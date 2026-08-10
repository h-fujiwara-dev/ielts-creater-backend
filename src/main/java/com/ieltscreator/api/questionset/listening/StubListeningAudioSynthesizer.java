package com.ieltscreator.api.questionset.listening;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.springframework.stereotype.Component;

/** Amazon Polly連携の実装に差し替えるまでの決定的なstub。テキストの語数から再生時間を算出し、 その長さの無音WAVファイルを生成する（実際の音声合成は行わない）。 */
@Component
public class StubListeningAudioSynthesizer implements ListeningAudioSynthesizer {

  private static final int SAMPLE_RATE = 8000;
  private static final int BITS_PER_SAMPLE = 16;
  private static final int CHANNELS = 1;
  private static final int BASE_DURATION_MS = 400;
  private static final int MS_PER_WORD = 300;

  @Override
  public SynthesizedAudio synthesize(String text, String voiceId) {
    int durationMs = BASE_DURATION_MS + wordCount(text) * MS_PER_WORD;
    return new SynthesizedAudio(buildSilentWav(durationMs), durationMs);
  }

  private static int wordCount(String text) {
    String trimmed = text == null ? "" : text.strip();
    return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
  }

  private static byte[] buildSilentWav(int durationMs) {
    int blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8);
    int byteRate = SAMPLE_RATE * blockAlign;
    int numSamples = SAMPLE_RATE * durationMs / 1000;
    int dataSize = numSamples * blockAlign;

    ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
    buffer.put("RIFF".getBytes());
    buffer.putInt(36 + dataSize);
    buffer.put("WAVE".getBytes());
    buffer.put("fmt ".getBytes());
    buffer.putInt(16);
    buffer.putShort((short) 1);
    buffer.putShort((short) CHANNELS);
    buffer.putInt(SAMPLE_RATE);
    buffer.putInt(byteRate);
    buffer.putShort((short) blockAlign);
    buffer.putShort((short) BITS_PER_SAMPLE);
    buffer.put("data".getBytes());
    buffer.putInt(dataSize);
    // 残りはデフォルトの0埋め = 無音データ
    return buffer.array();
  }
}

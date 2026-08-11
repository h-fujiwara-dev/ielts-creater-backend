package com.ieltscreator.api.questionset.listening;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 * Amazon Polly（Neural Engine）を呼び出す音声合成器（#00033）。Pollyから受け取るのはPCM生データのため、 {@link
 * StubListeningAudioSynthesizer}と同じ手法で44byte RIFF/WAVEヘッダを付与しWAVコンテナ化する （{@code
 * QuestionSetController}が{@code audio/wav}で配信するため必須）。呼び出し失敗時のリトライは AWS SDK組み込みの{@code
 * RetryStrategy}（{@code PollyClientConfig}側で設定）に委ね、ここでは行わない。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.generation", name = "mode", havingValue = "openai")
@RequiredArgsConstructor
public class PollyListeningAudioSynthesizer implements ListeningAudioSynthesizer {

  // Polly PCM出力がサポートするSampleRateは"8000"/"16000"のみ（Neural音声も同様、24000はMP3/OGG専用）
  private static final int SAMPLE_RATE = 16000;
  private static final int BITS_PER_SAMPLE = 16;
  private static final int CHANNELS = 1;

  private final PollyClient pollyClient;

  @Override
  public SynthesizedAudio synthesize(String text, String voiceId) {
    VoiceId resolvedVoiceId = VoiceId.fromValue(voiceId);
    if (resolvedVoiceId == VoiceId.UNKNOWN_TO_SDK_VERSION) {
      throw new IllegalArgumentException("Unsupported Polly voiceId: " + voiceId);
    }

    SynthesizeSpeechRequest request =
        SynthesizeSpeechRequest.builder()
            .text(text)
            .voiceId(resolvedVoiceId)
            .engine(Engine.NEURAL)
            .outputFormat(OutputFormat.PCM)
            .sampleRate(String.valueOf(SAMPLE_RATE))
            .build();

    long start = System.currentTimeMillis();
    try (ResponseInputStream<SynthesizeSpeechResponse> response =
        pollyClient.synthesizeSpeech(request)) {
      byte[] pcm = response.readAllBytes();
      int durationMs = (int) (pcm.length * 1000L / (SAMPLE_RATE * (BITS_PER_SAMPLE / 8)));
      log.info(
          "Polly synthesis succeeded: voiceId={}, durationMs={}, elapsedMs={}",
          voiceId,
          durationMs,
          System.currentTimeMillis() - start);
      return new SynthesizedAudio(wrapAsWav(pcm), durationMs);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read Polly audio stream", e);
    }
  }

  private static byte[] wrapAsWav(byte[] pcm) {
    int blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8);
    int byteRate = SAMPLE_RATE * blockAlign;
    int dataSize = pcm.length;

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
    buffer.put(pcm);
    return buffer.array();
  }
}

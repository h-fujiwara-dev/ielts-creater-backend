package com.ieltscreator.api.questionset.listening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;

@ExtendWith(MockitoExtension.class)
class PollyListeningAudioSynthesizerTest {

  @Mock private PollyClient pollyClient;

  private PollyListeningAudioSynthesizer synthesizer;

  @BeforeEach
  void setUp() {
    synthesizer = new PollyListeningAudioSynthesizer(pollyClient);
  }

  @Test
  void producesValidWavFromPcmAndSendsNeuralRequestParameters() {
    byte[] pcm = new byte[16000 * 2]; // 1 second of 16-bit mono silence at 16kHz
    when(pollyClient.synthesizeSpeech(any(SynthesizeSpeechRequest.class)))
        .thenReturn(
            new ResponseInputStream<>(
                SynthesizeSpeechResponse.builder().build(), new ByteArrayInputStream(pcm)));

    SynthesizedAudio audio = synthesizer.synthesize("Hello there", "Joanna");

    assertThat(audio.durationMs()).isEqualTo(1000);
    assertThat(audio.audioBytes().length).isEqualTo(44 + pcm.length);
    assertThat(new String(audio.audioBytes(), 0, 4)).isEqualTo("RIFF");
    assertThat(new String(audio.audioBytes(), 8, 4)).isEqualTo("WAVE");

    ArgumentCaptor<SynthesizeSpeechRequest> requestCaptor =
        ArgumentCaptor.forClass(SynthesizeSpeechRequest.class);
    verify(pollyClient).synthesizeSpeech(requestCaptor.capture());
    SynthesizeSpeechRequest sentRequest = requestCaptor.getValue();
    assertThat(sentRequest.text()).isEqualTo("Hello there");
    assertThat(sentRequest.voiceId()).isEqualTo(VoiceId.JOANNA);
    assertThat(sentRequest.engine()).isEqualTo(Engine.NEURAL);
    assertThat(sentRequest.outputFormat()).isEqualTo(OutputFormat.PCM);
    assertThat(sentRequest.sampleRate()).isEqualTo("16000");
  }

  @Test
  void rejectsUnsupportedVoiceIdWithoutCallingPolly() {
    assertThatThrownBy(() -> synthesizer.synthesize("Hello", "NotARealVoice"))
        .isInstanceOf(IllegalArgumentException.class);

    verify(pollyClient, never()).synthesizeSpeech(any(SynthesizeSpeechRequest.class));
  }

  @Test
  void propagatesPollyFailures() {
    when(pollyClient.synthesizeSpeech(any(SynthesizeSpeechRequest.class)))
        .thenThrow(SdkClientException.create("Polly unavailable"));

    assertThatThrownBy(() -> synthesizer.synthesize("Hello", "Joanna"))
        .isInstanceOf(SdkClientException.class);
  }
}

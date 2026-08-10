package com.ieltscreator.api.questionset.listening;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StubListeningAudioSynthesizerTest {

  private final StubListeningAudioSynthesizer synthesizer = new StubListeningAudioSynthesizer();

  @Test
  void producesValidWavHeaderWithPositiveDuration() {
    SynthesizedAudio audio = synthesizer.synthesize("Hello, how can I help you today?", "Joanna");

    assertThat(audio.durationMs()).isPositive();
    assertThat(audio.audioBytes().length).isGreaterThan(44);
    assertThat(new String(audio.audioBytes(), 0, 4)).isEqualTo("RIFF");
    assertThat(new String(audio.audioBytes(), 8, 4)).isEqualTo("WAVE");
  }

  @Test
  void longerTextProducesLongerDuration() {
    SynthesizedAudio shortAudio = synthesizer.synthesize("Hi.", "Joanna");
    SynthesizedAudio longAudio =
        synthesizer.synthesize(
            "This is a much longer sentence with many more words in it than the short one.",
            "Joanna");

    assertThat(longAudio.durationMs()).isGreaterThan(shortAudio.durationMs());
  }

  @Test
  void isDeterministicForTheSameInput() {
    SynthesizedAudio first = synthesizer.synthesize("Same text", "Joanna");
    SynthesizedAudio second = synthesizer.synthesize("Same text", "Joanna");

    assertThat(first.durationMs()).isEqualTo(second.durationMs());
    assertThat(first.audioBytes()).isEqualTo(second.audioBytes());
  }
}

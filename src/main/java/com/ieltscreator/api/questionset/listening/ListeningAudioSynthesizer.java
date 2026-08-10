package com.ieltscreator.api.questionset.listening;

/** 発話1件分の音声合成。実装はAmazon Polly連携版に差し替え可能。 */
public interface ListeningAudioSynthesizer {

  SynthesizedAudio synthesize(String text, String voiceId);
}

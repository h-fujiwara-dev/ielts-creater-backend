package com.ieltscreator.api.questionset.generation;

import com.ieltscreator.api.questionset.Difficulty;

/** Listening問題（台本＋設問）の生成。実装はOpenAI連携版に差し替え可能。 */
public interface ListeningQuestionGenerator {

  GeneratedListeningContent generate(String topic, Difficulty difficulty);
}

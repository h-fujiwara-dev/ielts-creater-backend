package com.ieltscreator.api.questionset.generation;

import com.ieltscreator.api.questionset.Difficulty;

/** Reading問題（パッセージ＋設問）の生成。実装はOpenAI連携版に差し替え可能。 */
public interface ReadingQuestionGenerator {

  GeneratedReadingContent generate(String topic, Difficulty difficulty);
}

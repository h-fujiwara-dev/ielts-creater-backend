package com.ieltscreator.api.questionset;

/** バンドスコア帯。QuestionSet.difficultyはString永続化のため、リクエスト検証専用のenum。 */
public enum Difficulty {
  BAND_4_5,
  BAND_5_6,
  BAND_6_7,
  BAND_7_8_PLUS
}

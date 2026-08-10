package com.ieltscreator.api.questionset.listening;

/** Listening音声ファイルの永続化。Phase1はローカルディスク、Phase3ではS3に差し替える。 */
public interface StorageService {

  /** contentを保存し、後でloadに渡せる保存先キーを返す。 */
  String save(String key, byte[] content);

  byte[] load(String key);
}

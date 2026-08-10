package com.ieltscreator.api.questionset.listening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDiskStorageServiceTest {

  @TempDir private Path tempDir;

  private LocalDiskStorageService storageService() {
    return new LocalDiskStorageService(tempDir.toString());
  }

  @Test
  void savesAndLoadsFileRoundTrip() {
    LocalDiskStorageService storageService = storageService();
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

    String key = storageService.save("questionSetId/0.wav", content);

    assertThat(storageService.load(key)).isEqualTo(content);
  }

  @Test
  void createsParentDirectoriesAsNeeded() {
    LocalDiskStorageService storageService = storageService();
    byte[] content = "nested".getBytes(StandardCharsets.UTF_8);

    storageService.save("a/b/c/0.wav", content);

    assertThat(tempDir.resolve("a/b/c/0.wav")).exists();
  }

  @Test
  void rejectsKeysThatEscapeTheRootDirectory() {
    LocalDiskStorageService storageService = storageService();

    assertThatThrownBy(() -> storageService.save("../escape.wav", new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

package com.ieltscreator.api.questionset.listening;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Phase1用のローカルディスク実装。 */
@Component
public class LocalDiskStorageService implements StorageService {

  private final Path rootDir;

  public LocalDiskStorageService(
      @Value("${app.storage.local-dir:./local-storage/audio-segments}") String localDir) {
    this.rootDir = Path.of(localDir).toAbsolutePath().normalize();
  }

  @Override
  public String save(String key, byte[] content) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, content);
      return key;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to save file for key: " + key, e);
    }
  }

  @Override
  public byte[] load(String key) {
    try {
      return Files.readAllBytes(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load file for key: " + key, e);
    }
  }

  private Path resolve(String key) {
    Path target = rootDir.resolve(key).normalize();
    if (!target.startsWith(rootDir)) {
      throw new IllegalArgumentException("Invalid storage key: " + key);
    }
    return target;
  }
}

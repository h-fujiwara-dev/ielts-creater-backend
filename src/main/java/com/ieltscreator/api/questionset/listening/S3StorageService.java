package com.ieltscreator.api.questionset.listening;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Phase3用のS3実装（実装規約.md 2章）。ECS Fargateはローカルディスクが永続化されないため本番はこちらを使う。 */
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "mode", havingValue = "s3")
public class S3StorageService implements StorageService {

  private final S3Client s3Client;
  private final String bucketName;

  public S3StorageService(
      S3Client s3Client, @Value("${app.storage.s3.bucket-name}") String bucketName) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
  }

  @Override
  public String save(String key, byte[] content) {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(key).build(),
        RequestBody.fromBytes(content));
    return key;
  }

  @Override
  public byte[] load(String key) {
    try (ResponseInputStream<GetObjectResponse> response =
        s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())) {
      return response.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load file for key: " + key, e);
    }
  }

  @Override
  public void delete(String key) {
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
  }
}

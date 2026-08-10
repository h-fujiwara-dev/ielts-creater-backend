package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.questionset.dto.AudioSegmentsResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateRequest;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetDetailResponse;
import com.ieltscreator.api.support.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * {@code question-set-fixture.sql}が各テストメソッド実行前にこのスイート自身の生成物をクリーンアップするため、
 * 各テストメソッドはこのスイート自身が作った分については「0件」から独立して開始できる（メソッド間の実行順に依存しない）。 ただし他の結合テストクラス（{@code
 * AttemptApiIntegrationTest}）のフィクスチャが同一devユーザーで
 * question_setを1件作成するため、当日カウントの起点は0または1になり得る。レート制限テストはこれに依存しないよう、 「何件目で拒否されるか」を実行時に観測する作りにしている。
 */
@Sql("/sql/question-set-fixture.sql")
class QuestionSetApiIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void topicExceedingMaxLengthIsRejected() {
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.READING, "a".repeat(101), Difficulty.BAND_6_7);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/question-sets", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void readingGenerationReachesReadyAndDetailIsRetrievable() throws InterruptedException {
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.READING, "Renewable energy", Difficulty.BAND_6_7);

    ResponseEntity<QuestionSetCreateResponse> createResponse =
        restTemplate.postForEntity(
            "/api/v1/question-sets", request, QuestionSetCreateResponse.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    UUID questionSetId = createResponse.getBody().id();
    assertThat(createResponse.getBody().status()).isEqualTo(QuestionSetStatus.GENERATING);
    assertThat(createResponse.getBody().topic()).isEqualTo("Renewable energy");

    QuestionSetDetailResponse detail = pollUntilReady(questionSetId);

    assertThat(detail.status()).isEqualTo(QuestionSetStatus.READY);
    assertThat(detail.passage()).isNotNull();
    assertThat(detail.passage().paragraphs()).isNotEmpty();
    assertThat(detail.questionGroups()).hasSize(4);
    assertThat(detail.questionGroups()).allSatisfy(g -> assertThat(g.questions()).isNotEmpty());
  }

  @Test
  void listeningGenerationReachesReadyWithDownloadableAudioSegments() throws InterruptedException {
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.LISTENING, "Marine biology", Difficulty.BAND_6_7);

    ResponseEntity<QuestionSetCreateResponse> createResponse =
        restTemplate.postForEntity(
            "/api/v1/question-sets", request, QuestionSetCreateResponse.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    UUID questionSetId = createResponse.getBody().id();

    QuestionSetDetailResponse detail = pollUntilReady(questionSetId);
    assertThat(detail.status()).isEqualTo(QuestionSetStatus.READY);
    assertThat(detail.listeningContext()).isNotBlank();
    assertThat(detail.passage()).isNull();

    ResponseEntity<AudioSegmentsResponse> segmentsResponse =
        restTemplate.getForEntity(
            "/api/v1/question-sets/{id}/audio-segments",
            AudioSegmentsResponse.class,
            questionSetId);
    assertThat(segmentsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(segmentsResponse.getBody().segments()).isNotEmpty();

    String fileUrl = segmentsResponse.getBody().segments().get(0).url();
    ResponseEntity<byte[]> fileResponse = restTemplate.getForEntity(fileUrl, byte[].class);
    assertThat(fileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fileResponse.getBody().length).isGreaterThan(44);
  }

  @Test
  void additionalGenerationRequestIsRateLimitedOnceDailyLimitIsReached()
      throws InterruptedException {
    // 当日カウントの起点（0件、または他クラスのフィクスチャ分の1件）に依存せず、拒否される（429）まで
    // リクエストを送り続け、それまでに受理された分はすべて完了を待ってから終了する。
    List<UUID> acceptedIds = new ArrayList<>();
    HttpStatusCode lastStatus = null;
    for (int attempt = 0; attempt < 5; attempt++) {
      ResponseEntity<QuestionSetCreateResponse> response = postReading("Attempt " + attempt);
      lastStatus = response.getStatusCode();
      if (!lastStatus.equals(HttpStatus.ACCEPTED)) {
        break;
      }
      acceptedIds.add(response.getBody().id());
    }

    assertThat(lastStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(acceptedIds).isNotEmpty();
    // 受理済み分の生成が、後続テストのフィクスチャクリーンアップと競合しないよう完了を待つ。
    for (UUID id : acceptedIds) {
      pollUntilReady(id);
    }
  }

  private ResponseEntity<QuestionSetCreateResponse> postReading(String topic) {
    return restTemplate.postForEntity(
        "/api/v1/question-sets",
        new QuestionSetCreateRequest(Section.READING, topic, Difficulty.BAND_6_7),
        QuestionSetCreateResponse.class);
  }

  @Test
  void getDetailForNonExistentQuestionSetReturnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/question-sets/{id}", String.class, UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private QuestionSetDetailResponse pollUntilReady(UUID questionSetId) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      ResponseEntity<QuestionSetDetailResponse> response =
          restTemplate.getForEntity(
              "/api/v1/question-sets/{id}", QuestionSetDetailResponse.class, questionSetId);
      QuestionSetDetailResponse body = response.getBody();
      if (body.status() != QuestionSetStatus.GENERATING) {
        return body;
      }
      Thread.sleep(100);
    }
    throw new IllegalStateException("Timed out waiting for question set to finish generating");
  }
}

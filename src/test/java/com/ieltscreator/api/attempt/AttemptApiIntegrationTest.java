package com.ieltscreator.api.attempt;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltscreator.api.attempt.dto.AttemptAnswerSaveRequest;
import com.ieltscreator.api.attempt.dto.AttemptAnswersResponse;
import com.ieltscreator.api.attempt.dto.AttemptHistoryPageResponse;
import com.ieltscreator.api.attempt.dto.AttemptResultResponse;
import com.ieltscreator.api.attempt.dto.AttemptStartRequest;
import com.ieltscreator.api.attempt.dto.AttemptStartResponse;
import com.ieltscreator.api.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

@Sql("/sql/attempt-fixture.sql")
class AttemptApiIntegrationTest extends AbstractIntegrationTest {

  private static final UUID QUESTION_SET_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID TFNG_QUESTION_ID =
      UUID.fromString("20000000-0000-0000-0000-000000001001");
  private static final UUID MCQ_QUESTION_ID =
      UUID.fromString("20000000-0000-0000-0000-000000001002");
  private static final UUID FILL_BLANK_QUESTION_ID =
      UUID.fromString("20000000-0000-0000-0000-000000001003");
  private static final UUID MATCHING_HEADINGS_QUESTION_ID =
      UUID.fromString("20000000-0000-0000-0000-000000001004");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void fullAttemptFlowGradesAllFourFormatsAndAppearsInHistory() {
    ResponseEntity<AttemptStartResponse> startResponse =
        restTemplate.postForEntity(
            "/api/v1/attempts",
            new AttemptStartRequest(QUESTION_SET_ID),
            AttemptStartResponse.class);
    assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID attemptId = startResponse.getBody().id();
    assertThat(startResponse.getBody().status()).isEqualTo(AttemptStatus.IN_PROGRESS);

    AttemptAnswerSaveRequest saveRequest =
        new AttemptAnswerSaveRequest(
            List.of(
                new AttemptAnswerSaveRequest.AnswerItem(TFNG_QUESTION_ID, "TRUE"),
                new AttemptAnswerSaveRequest.AnswerItem(MCQ_QUESTION_ID, "B"),
                new AttemptAnswerSaveRequest.AnswerItem(FILL_BLANK_QUESTION_ID, "CO2."),
                new AttemptAnswerSaveRequest.AnswerItem(MATCHING_HEADINGS_QUESTION_ID, "iii")));
    ResponseEntity<Void> saveResponse =
        restTemplate.exchange(
            "/api/v1/attempts/{id}/answers",
            HttpMethod.PATCH,
            new HttpEntity<>(saveRequest),
            Void.class,
            attemptId);
    assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<AttemptAnswersResponse> savedAnswers =
        restTemplate.getForEntity(
            "/api/v1/attempts/{id}/answers", AttemptAnswersResponse.class, attemptId);
    assertThat(savedAnswers.getBody().answers()).hasSize(4);

    ResponseEntity<AttemptResultResponse> submitResponse =
        restTemplate.postForEntity(
            "/api/v1/attempts/{id}/submit", null, AttemptResultResponse.class, attemptId);
    assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    AttemptResultResponse result = submitResponse.getBody();
    assertThat(result.maxScore()).isEqualTo(4);
    assertThat(result.rawScore()).isEqualTo(3);
    assertThat(isCorrect(result, TFNG_QUESTION_ID)).isTrue();
    assertThat(isCorrect(result, MCQ_QUESTION_ID)).isTrue();
    assertThat(isCorrect(result, FILL_BLANK_QUESTION_ID)).isTrue();
    assertThat(isCorrect(result, MATCHING_HEADINGS_QUESTION_ID)).isFalse();

    ResponseEntity<AttemptResultResponse> getResultResponse =
        restTemplate.getForEntity("/api/v1/attempts/{id}", AttemptResultResponse.class, attemptId);
    assertThat(getResultResponse.getBody().rawScore()).isEqualTo(3);

    ResponseEntity<AttemptHistoryPageResponse> historyResponse =
        restTemplate.getForEntity("/api/v1/attempts", AttemptHistoryPageResponse.class);
    assertThat(historyResponse.getBody().items())
        .anyMatch(item -> item.attemptId().equals(attemptId));
  }

  private static boolean isCorrect(AttemptResultResponse result, UUID questionId) {
    return result.answers().stream()
        .filter(answer -> answer.questionId().equals(questionId))
        .findFirst()
        .orElseThrow()
        .isCorrect();
  }
}

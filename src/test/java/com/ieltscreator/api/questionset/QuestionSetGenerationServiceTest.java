package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.exception.RateLimitExceededException;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateRequest;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionSetGenerationServiceTest {

  private static final List<String> TOPIC_PRESETS =
      List.of(
          "Environment",
          "Technology",
          "Education",
          "Health",
          "Travel",
          "Culture",
          "Science",
          "Work");

  @Mock private QuestionSetGenerationWorker questionSetGenerationWorker;
  @Mock private ExecutorService questionSetGenerationExecutor;
  @Mock private QuestionSetCreator questionSetCreator;

  private QuestionSetGenerationService service() {
    return new QuestionSetGenerationService(
        questionSetGenerationWorker, questionSetGenerationExecutor, questionSetCreator);
  }

  private static QuestionSet questionSet(UUID userId, String topic) {
    return QuestionSet.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .section(Section.READING)
        .topic(topic)
        .difficulty(Difficulty.BAND_6_7.name())
        .status(QuestionSetStatus.GENERATING)
        .promptVersion("stub-v1")
        .build();
  }

  @Test
  void propagatesRateLimitExceededFromCreator() {
    when(questionSetCreator.createWithinDailyLimit(any(), any(), anyString(), any(), anyString()))
        .thenThrow(
            new RateLimitExceededException("Daily question set generation limit (2) reached."));

    UUID userId = UUID.randomUUID();
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.READING, "Environment", Difficulty.BAND_6_7);

    assertThatThrownBy(() -> service().startGeneration(userId, request))
        .isInstanceOf(RateLimitExceededException.class);
    verify(questionSetGenerationExecutor, never()).submit(any(Runnable.class));
  }

  @Test
  void selectsRandomPresetTopicWhenTopicIsBlank() {
    UUID userId = UUID.randomUUID();
    when(questionSetCreator.createWithinDailyLimit(
            eq(userId), any(), anyString(), any(), anyString()))
        .thenAnswer(invocation -> questionSet(userId, invocation.getArgument(2, String.class)));

    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.READING, "  ", Difficulty.BAND_6_7);

    QuestionSetCreateResponse response = service().startGeneration(userId, request);

    assertThat(TOPIC_PRESETS).contains(response.topic());
    assertThat(response.status()).isEqualTo(QuestionSetStatus.GENERATING);
  }

  @Test
  void usesGivenTopicAsIsAndSubmitsGenerationTask() {
    UUID userId = UUID.randomUUID();
    when(questionSetCreator.createWithinDailyLimit(
            eq(userId), any(), anyString(), any(), anyString()))
        .thenAnswer(invocation -> questionSet(userId, invocation.getArgument(2, String.class)));

    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(
            Section.LISTENING, "Space exploration", Difficulty.BAND_7_8_PLUS);

    QuestionSetCreateResponse response = service().startGeneration(userId, request);

    assertThat(response.topic()).isEqualTo("Space exploration");
    verify(questionSetGenerationExecutor, times(1)).submit(any(Runnable.class));
  }
}

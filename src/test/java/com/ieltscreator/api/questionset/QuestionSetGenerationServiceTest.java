package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

  @Mock private QuestionSetRepository questionSetRepository;
  @Mock private QuestionSetGenerationWorker questionSetGenerationWorker;
  @Mock private ExecutorService questionSetGenerationExecutor;

  private QuestionSetGenerationService service() {
    return new QuestionSetGenerationService(
        questionSetRepository, questionSetGenerationWorker, questionSetGenerationExecutor);
  }

  @Test
  void throwsRateLimitExceededWhenDailyLimitReached() {
    when(questionSetRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(2L);

    UUID userId = UUID.randomUUID();
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.READING, "Environment", Difficulty.BAND_6_7);

    assertThatThrownBy(() -> service().startGeneration(userId, request))
        .isInstanceOf(RateLimitExceededException.class);
    verify(questionSetRepository, never()).save(any());
    verify(questionSetGenerationExecutor, never()).submit(any(Runnable.class));
  }

  @Test
  void selectsRandomPresetTopicWhenTopicIsBlank() {
    when(questionSetRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(0L);
    when(questionSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UUID userId = UUID.randomUUID();
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(Section.READING, "  ", Difficulty.BAND_6_7);

    QuestionSetCreateResponse response = service().startGeneration(userId, request);

    assertThat(TOPIC_PRESETS).contains(response.topic());
    assertThat(response.status()).isEqualTo(QuestionSetStatus.GENERATING);
  }

  @Test
  void usesGivenTopicAsIsAndSubmitsGenerationTask() {
    when(questionSetRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(1L);
    when(questionSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UUID userId = UUID.randomUUID();
    QuestionSetCreateRequest request =
        new QuestionSetCreateRequest(
            Section.LISTENING, "Space exploration", Difficulty.BAND_7_8_PLUS);

    QuestionSetCreateResponse response = service().startGeneration(userId, request);

    assertThat(response.topic()).isEqualTo("Space exploration");
    verify(questionSetGenerationExecutor, times(1)).submit(any(Runnable.class));
  }
}

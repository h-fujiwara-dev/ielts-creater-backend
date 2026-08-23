package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.exception.RateLimitExceededException;
import com.ieltscreator.api.user.AppUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionSetCreatorTest {

  @Mock private QuestionSetRepository questionSetRepository;
  @Mock private AppUserRepository appUserRepository;

  private QuestionSetCreator creator() {
    return new QuestionSetCreator(questionSetRepository, appUserRepository);
  }

  @Test
  void throwsRateLimitExceededWhenDailyLimitReached() {
    UUID userId = UUID.randomUUID();
    when(appUserRepository.existsByIdAndIsGuestTrue(userId)).thenReturn(false);
    when(questionSetRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(2L);

    assertThatThrownBy(
            () ->
                creator()
                    .createWithinDailyLimit(
                        userId, Section.READING, "Environment", Difficulty.BAND_6_7, "stub-v1"))
        .isInstanceOf(RateLimitExceededException.class);
    verify(questionSetRepository, never()).save(any());
  }

  @Test
  void acquiresLockBeforeCountingWhenNotGuest() {
    UUID userId = UUID.randomUUID();
    when(appUserRepository.existsByIdAndIsGuestTrue(userId)).thenReturn(false);
    when(questionSetRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(0L);
    when(questionSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    creator()
        .createWithinDailyLimit(
            userId, Section.READING, "Environment", Difficulty.BAND_6_7, "stub-v1");

    InOrder inOrder = Mockito.inOrder(questionSetRepository);
    inOrder.verify(questionSetRepository).acquireDailyLimitLock(userId);
    inOrder.verify(questionSetRepository).countByUserIdAndCreatedAtBetween(any(), any(), any());
    inOrder.verify(questionSetRepository).save(any());
  }

  @Test
  void bypassesDailyLimitAndLockForGuestUser() {
    UUID userId = UUID.randomUUID();
    when(appUserRepository.existsByIdAndIsGuestTrue(userId)).thenReturn(true);
    when(questionSetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    QuestionSet created =
        creator()
            .createWithinDailyLimit(
                userId, Section.READING, "Environment", Difficulty.BAND_6_7, "stub-v1");

    assertThat(created.getTopic()).isEqualTo("Environment");
    verify(questionSetRepository, never()).acquireDailyLimitLock(any());
    verify(questionSetRepository, never()).countByUserIdAndCreatedAtBetween(any(), any(), any());
  }
}

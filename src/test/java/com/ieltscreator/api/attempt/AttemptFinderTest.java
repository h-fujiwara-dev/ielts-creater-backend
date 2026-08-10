package com.ieltscreator.api.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttemptFinderTest {

  @Mock private AttemptRepository attemptRepository;

  private AttemptFinder finder() {
    return new AttemptFinder(attemptRepository);
  }

  @Test
  void returnsAttemptWhenOwnedByUser() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).userId(userId).build();
    when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

    assertThat(finder().findOwned(userId, attemptId)).isEqualTo(attempt);
  }

  @Test
  void throwsWhenAttemptDoesNotExist() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    when(attemptRepository.findById(attemptId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> finder().findOwned(userId, attemptId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void throwsWhenAttemptBelongsToAnotherUser() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).userId(otherUserId).build();
    when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

    assertThatThrownBy(() -> finder().findOwned(userId, attemptId))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

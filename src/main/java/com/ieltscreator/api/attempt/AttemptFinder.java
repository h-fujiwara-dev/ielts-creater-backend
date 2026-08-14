package com.ieltscreator.api.attempt;

import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** attemptIdの存在確認と所有者チェックを一箇所にまとめる（他ユーザーのAttemptは404）。 */
@Component
@RequiredArgsConstructor
class AttemptFinder {

  private final AttemptRepository attemptRepository;

  Attempt findOwned(UUID userId, UUID attemptId) {
    return attemptRepository
        .findById(attemptId)
        .filter(attempt -> attempt.getUserId().equals(userId))
        .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
  }
}

package com.ieltscreator.api.attempt;

import com.ieltscreator.api.attempt.dto.AttemptAnswerSaveRequest;
import com.ieltscreator.api.attempt.dto.AttemptAnswersResponse;
import com.ieltscreator.api.attempt.dto.AttemptHistoryPageResponse;
import com.ieltscreator.api.attempt.dto.AttemptResultResponse;
import com.ieltscreator.api.attempt.dto.AttemptStartRequest;
import com.ieltscreator.api.attempt.dto.AttemptStartResponse;
import com.ieltscreator.api.common.security.CurrentUserProvider;
import com.ieltscreator.api.questionset.Section;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
public class AttemptController {

  private final AttemptService attemptService;
  private final AttemptSubmissionService attemptSubmissionService;
  private final CurrentUserProvider currentUserProvider;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AttemptStartResponse start(@Valid @RequestBody AttemptStartRequest request) {
    return attemptService.start(currentUserProvider.currentUserId(), request);
  }

  @PatchMapping("/{id}/answers")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveAnswers(
      @PathVariable UUID id, @Valid @RequestBody AttemptAnswerSaveRequest request) {
    attemptService.saveAnswers(currentUserProvider.currentUserId(), id, request);
  }

  @GetMapping("/{id}/answers")
  public AttemptAnswersResponse getSavedAnswers(@PathVariable UUID id) {
    return attemptService.getSavedAnswers(currentUserProvider.currentUserId(), id);
  }

  @PostMapping("/{id}/submit")
  public AttemptResultResponse submit(@PathVariable UUID id) {
    return attemptSubmissionService.submit(currentUserProvider.currentUserId(), id);
  }

  @GetMapping("/{id}")
  public AttemptResultResponse getResult(@PathVariable UUID id) {
    return attemptSubmissionService.getResult(currentUserProvider.currentUserId(), id);
  }

  @GetMapping
  public AttemptHistoryPageResponse history(
      @RequestParam(required = false) Section section,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return attemptService.getHistory(currentUserProvider.currentUserId(), section, page, size);
  }
}

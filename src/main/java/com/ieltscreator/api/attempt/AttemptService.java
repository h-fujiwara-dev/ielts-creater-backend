package com.ieltscreator.api.attempt;

import com.ieltscreator.api.attempt.dto.AttemptAnswerSaveRequest;
import com.ieltscreator.api.attempt.dto.AttemptAnswersResponse;
import com.ieltscreator.api.attempt.dto.AttemptHistoryItemResponse;
import com.ieltscreator.api.attempt.dto.AttemptHistoryPageResponse;
import com.ieltscreator.api.attempt.dto.AttemptStartRequest;
import com.ieltscreator.api.attempt.dto.AttemptStartResponse;
import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import com.ieltscreator.api.questionset.QuestionSet;
import com.ieltscreator.api.questionset.QuestionSetRepository;
import com.ieltscreator.api.questionset.Section;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttemptService {

  private final AttemptFinder attemptFinder;
  private final AttemptRepository attemptRepository;
  private final AttemptAnswerRepository attemptAnswerRepository;
  private final QuestionSetRepository questionSetRepository;

  @Transactional
  public AttemptStartResponse start(UUID userId, AttemptStartRequest request) {
    QuestionSet questionSet =
        questionSetRepository
            .findById(request.questionSetId())
            .filter(qs -> qs.getUserId().equals(userId))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "QuestionSet not found: " + request.questionSetId()));

    Attempt attempt =
        Attempt.builder()
            .userId(userId)
            .questionSetId(questionSet.getId())
            .status(AttemptStatus.IN_PROGRESS)
            .build();
    Attempt saved = attemptRepository.save(attempt);
    return new AttemptStartResponse(saved.getId(), saved.getStatus());
  }

  @Transactional
  public void saveAnswers(UUID userId, UUID attemptId, AttemptAnswerSaveRequest request) {
    Attempt attempt = attemptFinder.findOwned(userId, attemptId);
    for (AttemptAnswerSaveRequest.AnswerItem item : request.answers()) {
      AttemptAnswer answer =
          attemptAnswerRepository
              .findByAttemptIdAndQuestionId(attempt.getId(), item.questionId())
              .orElseGet(
                  () ->
                      AttemptAnswer.builder()
                          .attemptId(attempt.getId())
                          .questionId(item.questionId())
                          .build());
      answer.setUserAnswerText(item.userAnswerText());
      attemptAnswerRepository.save(answer);
    }
  }

  @Transactional(readOnly = true)
  public AttemptAnswersResponse getSavedAnswers(UUID userId, UUID attemptId) {
    Attempt attempt = attemptFinder.findOwned(userId, attemptId);
    List<AttemptAnswersResponse.SavedAnswer> answers =
        attemptAnswerRepository.findAllByAttemptId(attempt.getId()).stream()
            .map(
                a ->
                    new AttemptAnswersResponse.SavedAnswer(
                        a.getQuestionId(), a.getUserAnswerText()))
            .toList();
    return new AttemptAnswersResponse(attempt.getId(), attempt.getStatus(), answers);
  }

  @Transactional(readOnly = true)
  public AttemptHistoryPageResponse getHistory(UUID userId, Section section, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AttemptHistoryItemResponse> result =
        attemptRepository.findHistory(userId, section, pageable);
    return new AttemptHistoryPageResponse(
        result.getContent(), result.getNumber(), result.getTotalPages());
  }
}

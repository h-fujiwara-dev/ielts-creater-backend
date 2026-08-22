package com.ieltscreator.api.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.attempt.dto.AttemptAnswerSaveRequest;
import com.ieltscreator.api.attempt.dto.AttemptAnswersResponse;
import com.ieltscreator.api.attempt.dto.AttemptHistoryItemResponse;
import com.ieltscreator.api.attempt.dto.AttemptStartRequest;
import com.ieltscreator.api.attempt.dto.AttemptStartResponse;
import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import com.ieltscreator.api.questionset.QuestionSet;
import com.ieltscreator.api.questionset.QuestionSetRepository;
import com.ieltscreator.api.questionset.Section;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

  @Mock private AttemptFinder attemptFinder;
  @Mock private AttemptRepository attemptRepository;
  @Mock private AttemptAnswerRepository attemptAnswerRepository;
  @Mock private QuestionSetRepository questionSetRepository;

  private AttemptService service() {
    return new AttemptService(
        attemptFinder, attemptRepository, attemptAnswerRepository, questionSetRepository);
  }

  @Test
  void startsAttemptForOwnedQuestionSet() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    QuestionSet questionSet = QuestionSet.builder().id(questionSetId).userId(userId).build();
    when(questionSetRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
    UUID attemptId = UUID.randomUUID();
    when(attemptRepository.save(any(Attempt.class)))
        .thenAnswer(
            invocation -> {
              Attempt attempt = invocation.getArgument(0);
              attempt.setId(attemptId);
              return attempt;
            });

    AttemptStartResponse response = service().start(userId, new AttemptStartRequest(questionSetId));

    assertThat(response.id()).isEqualTo(attemptId);
    assertThat(response.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
    ArgumentCaptor<Attempt> attemptCaptor = ArgumentCaptor.forClass(Attempt.class);
    verify(attemptRepository).save(attemptCaptor.capture());
    assertThat(attemptCaptor.getValue().getUserId()).isEqualTo(userId);
    assertThat(attemptCaptor.getValue().getQuestionSetId()).isEqualTo(questionSetId);
  }

  @Test
  void throwsWhenStartingAttemptForQuestionSetOwnedByAnotherUser() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    QuestionSet questionSet =
        QuestionSet.builder().id(questionSetId).userId(UUID.randomUUID()).build();
    when(questionSetRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));

    assertThatThrownBy(() -> service().start(userId, new AttemptStartRequest(questionSetId)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void throwsWhenStartingAttemptForMissingQuestionSet() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    when(questionSetRepository.findById(questionSetId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().start(userId, new AttemptStartRequest(questionSetId)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createsNewAnswerWhenNoneSavedYet() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).userId(userId).build();
    when(attemptFinder.findOwned(userId, attemptId)).thenReturn(attempt);
    when(attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
        .thenReturn(Optional.empty());

    service()
        .saveAnswers(
            userId,
            attemptId,
            new AttemptAnswerSaveRequest(
                List.of(new AttemptAnswerSaveRequest.AnswerItem(questionId, "answer"))));

    ArgumentCaptor<AttemptAnswer> answerCaptor = ArgumentCaptor.forClass(AttemptAnswer.class);
    verify(attemptAnswerRepository).save(answerCaptor.capture());
    assertThat(answerCaptor.getValue().getAttemptId()).isEqualTo(attemptId);
    assertThat(answerCaptor.getValue().getQuestionId()).isEqualTo(questionId);
    assertThat(answerCaptor.getValue().getUserAnswerText()).isEqualTo("answer");
  }

  @Test
  void overwritesExistingAnswerForSameQuestion() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    UUID answerId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).userId(userId).build();
    when(attemptFinder.findOwned(userId, attemptId)).thenReturn(attempt);
    AttemptAnswer existing =
        AttemptAnswer.builder()
            .id(answerId)
            .attemptId(attemptId)
            .questionId(questionId)
            .userAnswerText("old")
            .build();
    when(attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
        .thenReturn(Optional.of(existing));

    service()
        .saveAnswers(
            userId,
            attemptId,
            new AttemptAnswerSaveRequest(
                List.of(new AttemptAnswerSaveRequest.AnswerItem(questionId, "new"))));

    ArgumentCaptor<AttemptAnswer> answerCaptor = ArgumentCaptor.forClass(AttemptAnswer.class);
    verify(attemptAnswerRepository).save(answerCaptor.capture());
    assertThat(answerCaptor.getValue().getId()).isEqualTo(answerId);
    assertThat(answerCaptor.getValue().getUserAnswerText()).isEqualTo("new");
  }

  @Test
  void throwsWhenSavingAnswersForAttemptOwnedByAnotherUser() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    when(attemptFinder.findOwned(userId, attemptId))
        .thenThrow(new ResourceNotFoundException("Attempt not found: " + attemptId));

    assertThatThrownBy(
            () ->
                service()
                    .saveAnswers(
                        userId,
                        attemptId,
                        new AttemptAnswerSaveRequest(
                            List.of(
                                new AttemptAnswerSaveRequest.AnswerItem(
                                    UUID.randomUUID(), "answer")))))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void returnsSavedAnswersForOwnedAttempt() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    Attempt attempt =
        Attempt.builder().id(attemptId).userId(userId).status(AttemptStatus.IN_PROGRESS).build();
    when(attemptFinder.findOwned(userId, attemptId)).thenReturn(attempt);
    when(attemptAnswerRepository.findAllByAttemptId(attemptId))
        .thenReturn(
            List.of(
                AttemptAnswer.builder()
                    .attemptId(attemptId)
                    .questionId(questionId)
                    .userAnswerText("answer")
                    .build()));

    AttemptAnswersResponse response = service().getSavedAnswers(userId, attemptId);

    assertThat(response.attemptId()).isEqualTo(attemptId);
    assertThat(response.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
    assertThat(response.answers())
        .containsExactly(new AttemptAnswersResponse.SavedAnswer(questionId, "answer"));
  }

  @Test
  void returnsHistoryPageFilteredBySection() {
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Page<AttemptHistoryItemResponse> page = new PageImpl<>(List.of(), pageable, 0);
    when(attemptRepository.findHistory(userId, Section.READING, pageable)).thenReturn(page);

    var response = service().getHistory(userId, Section.READING, 0, 20);

    assertThat(response.items()).isEmpty();
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.totalPages()).isEqualTo(0);
  }
}

package com.ieltscreator.api.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.attempt.dto.AttemptAnswerResult;
import com.ieltscreator.api.attempt.dto.AttemptResultResponse;
import com.ieltscreator.api.attempt.grading.AnswerGrader;
import com.ieltscreator.api.attempt.grading.AnswerKeyCodec;
import com.ieltscreator.api.attempt.grading.GraderFactory;
import com.ieltscreator.api.common.exception.ValidationException;
import com.ieltscreator.api.questionset.Question;
import com.ieltscreator.api.questionset.QuestionFormatType;
import com.ieltscreator.api.questionset.QuestionGroup;
import com.ieltscreator.api.questionset.QuestionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttemptSubmissionServiceTest {

  @Mock private AttemptFinder attemptFinder;
  @Mock private AttemptAnswerRepository attemptAnswerRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private GraderFactory graderFactory;
  @Mock private AnswerGrader answerGrader;

  private final AnswerKeyCodec answerKeyCodec = new AnswerKeyCodec(new ObjectMapper());

  private AttemptSubmissionService service() {
    return new AttemptSubmissionService(
        attemptFinder, attemptAnswerRepository, questionRepository, graderFactory, answerKeyCodec);
  }

  @Test
  void submitCountsUnansweredQuestionsAsIncorrectAndComputesMaxScoreFromFullQuestionSet() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    UUID answeredQuestionId = UUID.randomUUID();
    UUID unansweredQuestionId = UUID.randomUUID();

    Attempt attempt =
        Attempt.builder().id(attemptId).userId(userId).questionSetId(questionSetId).build();
    when(attemptFinder.findOwned(userId, attemptId)).thenReturn(attempt);

    QuestionGroup group =
        QuestionGroup.builder().formatType(QuestionFormatType.TFNG).displayOrder(1).build();
    Question answeredQuestion =
        Question.builder()
            .id(answeredQuestionId)
            .questionGroup(group)
            .displayOrder(1)
            .correctAnswerKey("\"TRUE\"")
            .build();
    Question unansweredQuestion =
        Question.builder()
            .id(unansweredQuestionId)
            .questionGroup(group)
            .displayOrder(2)
            .correctAnswerKey("\"FALSE\"")
            .build();
    when(questionRepository.findAllByQuestionGroup_QuestionSetId(questionSetId))
        .thenReturn(List.of(answeredQuestion, unansweredQuestion));

    AttemptAnswer savedAnswer =
        AttemptAnswer.builder()
            .attemptId(attemptId)
            .questionId(answeredQuestionId)
            .userAnswerText("TRUE")
            .build();
    when(attemptAnswerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(savedAnswer));

    when(graderFactory.resolve(QuestionFormatType.TFNG)).thenReturn(answerGrader);
    when(answerGrader.isCorrect(answeredQuestion, "TRUE")).thenReturn(true);
    when(answerGrader.isCorrect(unansweredQuestion, null)).thenReturn(false);

    AttemptResultResponse response = service().submit(userId, attemptId);

    assertThat(response.rawScore()).isEqualTo(1);
    assertThat(response.maxScore()).isEqualTo(2);
    assertThat(response.answers()).hasSize(2);
    assertThat(response.answers().get(1).userAnswerText()).isNull();
    assertThat(response.answers().get(1).isCorrect()).isFalse();
    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
  }

  @Test
  void getResultThrowsWhenAttemptIsNotSubmittedYet() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    Attempt attempt =
        Attempt.builder().id(attemptId).userId(userId).status(AttemptStatus.IN_PROGRESS).build();
    when(attemptFinder.findOwned(userId, attemptId)).thenReturn(attempt);

    assertThatThrownBy(() -> service().getResult(userId, attemptId))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void getResultReconstructsAnswersFromSnapshot() {
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();

    Attempt attempt =
        Attempt.builder()
            .id(attemptId)
            .userId(userId)
            .status(AttemptStatus.SUBMITTED)
            .rawScore(1)
            .maxScore(1)
            .build();
    when(attemptFinder.findOwned(userId, attemptId)).thenReturn(attempt);

    AttemptAnswer answer =
        AttemptAnswer.builder()
            .attemptId(attemptId)
            .questionId(questionId)
            .userAnswerText("TRUE")
            .isCorrect(true)
            .correctAnswerSnapshot("\"TRUE\"")
            .build();
    when(attemptAnswerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(answer));

    QuestionGroup group =
        QuestionGroup.builder().formatType(QuestionFormatType.TFNG).displayOrder(1).build();
    Question question =
        Question.builder()
            .id(questionId)
            .questionGroup(group)
            .displayOrder(1)
            .explanation("because")
            .build();
    when(questionRepository.findAllById(List.of(questionId))).thenReturn(List.of(question));

    AttemptResultResponse response = service().getResult(userId, attemptId);

    assertThat(response.answers()).hasSize(1);
    AttemptAnswerResult result = response.answers().get(0);
    assertThat(result.correctAnswer()).isEqualTo("TRUE");
    assertThat(result.explanation()).isEqualTo("because");
  }
}

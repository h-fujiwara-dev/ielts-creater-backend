package com.ieltscreator.api.attempt;

import com.ieltscreator.api.attempt.dto.AttemptAnswerResult;
import com.ieltscreator.api.attempt.dto.AttemptResultResponse;
import com.ieltscreator.api.attempt.grading.AnswerGrader;
import com.ieltscreator.api.attempt.grading.AnswerKeyCodec;
import com.ieltscreator.api.attempt.grading.GraderFactory;
import com.ieltscreator.api.common.exception.ValidationException;
import com.ieltscreator.api.questionset.Question;
import com.ieltscreator.api.questionset.QuestionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttemptSubmissionService {

  private static final Comparator<Question> DISPLAY_ORDER =
      Comparator.<Question>comparingInt(q -> q.getQuestionGroup().getDisplayOrder())
          .thenComparingInt(Question::getDisplayOrder);

  private final AttemptFinder attemptFinder;
  private final AttemptAnswerRepository attemptAnswerRepository;
  private final QuestionRepository questionRepository;
  private final GraderFactory graderFactory;
  private final AnswerKeyCodec answerKeyCodec;

  @Transactional
  public AttemptResultResponse submit(UUID userId, UUID attemptId) {
    Attempt attempt = attemptFinder.findOwned(userId, attemptId);

    List<Question> questions =
        questionRepository.findAllByQuestionGroup_QuestionSetId(attempt.getQuestionSetId()).stream()
            .sorted(DISPLAY_ORDER)
            .toList();
    Map<UUID, AttemptAnswer> savedAnswersByQuestionId =
        attemptAnswerRepository.findAllByAttemptId(attempt.getId()).stream()
            .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity()));

    int rawScore = 0;
    List<AttemptAnswerResult> results = new ArrayList<>();
    for (Question question : questions) {
      AttemptAnswer answer =
          savedAnswersByQuestionId.computeIfAbsent(
              question.getId(),
              questionId ->
                  AttemptAnswer.builder()
                      .attemptId(attempt.getId())
                      .questionId(questionId)
                      .build());

      AnswerGrader grader = graderFactory.resolve(question.getQuestionGroup().getFormatType());
      boolean correct = grader.isCorrect(question, answer.getUserAnswerText());
      if (correct) {
        rawScore++;
      }

      answer.setIsCorrect(correct);
      answer.setCorrectAnswerSnapshot(question.getCorrectAnswerKey());
      attemptAnswerRepository.save(answer);

      results.add(
          new AttemptAnswerResult(
              question.getId(),
              answer.getUserAnswerText(),
              correct,
              answerKeyCodec.asDisplayText(question.getCorrectAnswerKey()),
              question.getExplanation()));
    }

    attempt.setStatus(AttemptStatus.SUBMITTED);
    attempt.setSubmittedAt(Instant.now());
    attempt.setRawScore(rawScore);
    attempt.setMaxScore(questions.size());

    return new AttemptResultResponse(attempt.getId(), rawScore, questions.size(), results);
  }

  @Transactional(readOnly = true)
  public AttemptResultResponse getResult(UUID userId, UUID attemptId) {
    Attempt attempt = attemptFinder.findOwned(userId, attemptId);
    if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
      throw new ValidationException("Attempt is not submitted yet: " + attemptId);
    }

    List<AttemptAnswer> answers = attemptAnswerRepository.findAllByAttemptId(attempt.getId());
    Map<UUID, Question> questionsById =
        questionRepository
            .findAllById(answers.stream().map(AttemptAnswer::getQuestionId).toList())
            .stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));

    List<AttemptAnswerResult> results =
        answers.stream()
            .sorted(Comparator.comparing(a -> questionsById.get(a.getQuestionId()), DISPLAY_ORDER))
            .map(
                answer -> {
                  Question question = questionsById.get(answer.getQuestionId());
                  String correctAnswer =
                      answer.getCorrectAnswerSnapshot() != null
                          ? answerKeyCodec.asDisplayText(answer.getCorrectAnswerSnapshot())
                          : null;
                  return new AttemptAnswerResult(
                      answer.getQuestionId(),
                      answer.getUserAnswerText(),
                      answer.getIsCorrect(),
                      correctAnswer,
                      question.getExplanation());
                })
            .toList();

    return new AttemptResultResponse(
        attempt.getId(), attempt.getRawScore(), attempt.getMaxScore(), results);
  }
}

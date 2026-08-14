package com.ieltscreator.api.questionset;

import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import com.ieltscreator.api.questionset.dto.AudioSegmentsResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetDetailResponse;
import com.ieltscreator.api.questionset.listening.StorageService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionSetQueryService {

  private final QuestionSetFinder questionSetFinder;
  private final PassageRepository passageRepository;
  private final ListeningScriptRepository listeningScriptRepository;
  private final QuestionGroupRepository questionGroupRepository;
  private final QuestionRepository questionRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final AudioSegmentRepository audioSegmentRepository;
  private final QuestionSetMapper questionSetMapper;
  private final StorageService storageService;

  @Transactional(readOnly = true)
  public QuestionSetDetailResponse getDetail(UUID userId, UUID questionSetId) {
    QuestionSet questionSet = questionSetFinder.findOwned(userId, questionSetId);

    Passage passage = passageRepository.findByQuestionSetId(questionSetId).orElse(null);
    ListeningScript listeningScript =
        listeningScriptRepository.findByQuestionSetId(questionSetId).orElse(null);
    List<QuestionGroup> groups =
        questionGroupRepository.findAllByQuestionSetIdOrderByDisplayOrderAsc(questionSetId);
    List<Question> questions =
        questionRepository.findAllByQuestionGroup_QuestionSetId(questionSetId);

    Map<UUID, List<Question>> questionsByGroupId =
        questions.stream().collect(Collectors.groupingBy(q -> q.getQuestionGroup().getId()));
    List<UUID> questionIds = questions.stream().map(Question::getId).toList();
    Map<UUID, List<AnswerOption>> optionsByQuestionId =
        questionIds.isEmpty()
            ? Map.of()
            : answerOptionRepository.findAllByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(AnswerOption::getQuestionId));

    return questionSetMapper.toDetailResponse(
        questionSet, passage, listeningScript, groups, questionsByGroupId, optionsByQuestionId);
  }

  @Transactional(readOnly = true)
  public AudioSegmentsResponse getAudioSegments(UUID userId, UUID questionSetId) {
    questionSetFinder.findOwned(userId, questionSetId);
    List<AudioSegment> segments =
        audioSegmentRepository.findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(
            questionSetId);
    return questionSetMapper.toAudioSegmentsResponse(questionSetId, segments);
  }

  @Transactional(readOnly = true)
  public byte[] loadAudioFile(UUID userId, UUID questionSetId, UUID audioSegmentId) {
    questionSetFinder.findOwned(userId, questionSetId);
    AudioSegment segment =
        audioSegmentRepository
            .findByIdAndListeningScript_QuestionSetId(audioSegmentId, questionSetId)
            .orElseThrow(
                () -> new ResourceNotFoundException("AudioSegment not found: " + audioSegmentId));
    return storageService.load(segment.getS3Key());
  }
}

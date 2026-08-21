package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.exception.ResourceNotFoundException;
import com.ieltscreator.api.questionset.dto.AudioSegmentItemResponse;
import com.ieltscreator.api.questionset.dto.AudioSegmentsResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetDetailResponse;
import com.ieltscreator.api.questionset.listening.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionSetQueryServiceTest {

  @Mock private QuestionSetFinder questionSetFinder;
  @Mock private PassageRepository passageRepository;
  @Mock private ListeningScriptRepository listeningScriptRepository;
  @Mock private QuestionGroupRepository questionGroupRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private AnswerOptionRepository answerOptionRepository;
  @Mock private AudioSegmentRepository audioSegmentRepository;
  @Mock private QuestionSetMapper questionSetMapper;
  @Mock private StorageService storageService;

  private QuestionSetQueryService service() {
    return new QuestionSetQueryService(
        questionSetFinder,
        passageRepository,
        listeningScriptRepository,
        questionGroupRepository,
        questionRepository,
        answerOptionRepository,
        audioSegmentRepository,
        questionSetMapper,
        storageService);
  }

  @Test
  void getDetailDelegatesToMapperAfterOwnershipCheck() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    QuestionSet questionSet = QuestionSet.builder().id(questionSetId).userId(userId).build();
    when(questionSetFinder.findOwned(userId, questionSetId)).thenReturn(questionSet);
    when(passageRepository.findByQuestionSetId(questionSetId)).thenReturn(Optional.empty());
    when(listeningScriptRepository.findByQuestionSetId(questionSetId)).thenReturn(Optional.empty());
    when(questionGroupRepository.findAllByQuestionSetIdOrderByDisplayOrderAsc(questionSetId))
        .thenReturn(List.of());
    when(questionRepository.findAllByQuestionGroup_QuestionSetId(questionSetId))
        .thenReturn(List.of());
    QuestionSetDetailResponse expected =
        new QuestionSetDetailResponse(questionSetId, null, null, null, null, null, null, List.of());
    when(questionSetMapper.toDetailResponse(any(), any(), any(), anyList(), any(), any()))
        .thenReturn(expected);

    QuestionSetDetailResponse response = service().getDetail(userId, questionSetId);

    assertThat(response).isEqualTo(expected);
  }

  @Test
  void getDetailPropagatesNotFoundWithoutQueryingOtherRepositories() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    when(questionSetFinder.findOwned(userId, questionSetId))
        .thenThrow(new ResourceNotFoundException("QuestionSet not found: " + questionSetId));

    assertThatThrownBy(() -> service().getDetail(userId, questionSetId))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(passageRepository, never()).findByQuestionSetId(any());
  }

  @Test
  void getAudioSegmentsChecksOwnershipBeforeReturningSegments() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    QuestionSet questionSet = QuestionSet.builder().id(questionSetId).userId(userId).build();
    when(questionSetFinder.findOwned(userId, questionSetId)).thenReturn(questionSet);
    when(audioSegmentRepository.findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(
            questionSetId))
        .thenReturn(List.of());
    AudioSegmentsResponse expected =
        new AudioSegmentsResponse(List.of(new AudioSegmentItemResponse(0, "/url", 1000)));
    when(questionSetMapper.toAudioSegmentsResponse(questionSetId, List.of())).thenReturn(expected);

    AudioSegmentsResponse response = service().getAudioSegments(userId, questionSetId);

    assertThat(response).isEqualTo(expected);
  }

  @Test
  void loadAudioFileReturnsBytesFromStorageServiceWhenSegmentExists() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    UUID audioSegmentId = UUID.randomUUID();
    QuestionSet questionSet = QuestionSet.builder().id(questionSetId).userId(userId).build();
    when(questionSetFinder.findOwned(userId, questionSetId)).thenReturn(questionSet);
    AudioSegment segment = AudioSegment.builder().id(audioSegmentId).s3Key("audio/key.mp3").build();
    when(audioSegmentRepository.findByIdAndListeningScript_QuestionSetId(
            audioSegmentId, questionSetId))
        .thenReturn(Optional.of(segment));
    byte[] fileContent = {1, 2, 3};
    when(storageService.load("audio/key.mp3")).thenReturn(fileContent);

    byte[] result = service().loadAudioFile(userId, questionSetId, audioSegmentId);

    assertThat(result).isEqualTo(fileContent);
  }

  @Test
  void loadAudioFileThrowsWhenSegmentDoesNotBelongToQuestionSet() {
    UUID userId = UUID.randomUUID();
    UUID questionSetId = UUID.randomUUID();
    UUID audioSegmentId = UUID.randomUUID();
    QuestionSet questionSet = QuestionSet.builder().id(questionSetId).userId(userId).build();
    when(questionSetFinder.findOwned(userId, questionSetId)).thenReturn(questionSet);
    when(audioSegmentRepository.findByIdAndListeningScript_QuestionSetId(
            audioSegmentId, questionSetId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().loadAudioFile(userId, questionSetId, audioSegmentId))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(storageService, never()).load(any());
  }
}

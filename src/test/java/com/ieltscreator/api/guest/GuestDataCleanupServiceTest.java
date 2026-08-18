package com.ieltscreator.api.guest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.questionset.AudioSegment;
import com.ieltscreator.api.questionset.AudioSegmentRepository;
import com.ieltscreator.api.questionset.QuestionSet;
import com.ieltscreator.api.questionset.QuestionSetRepository;
import com.ieltscreator.api.questionset.listening.StorageService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestDataCleanupServiceTest {

  @Mock private QuestionSetRepository questionSetRepository;
  @Mock private AudioSegmentRepository audioSegmentRepository;
  @Mock private StorageService storageService;
  @Mock private GuestIpQuotaRepository guestIpQuotaRepository;

  private GuestDataCleanupService service() {
    return new GuestDataCleanupService(
        questionSetRepository, audioSegmentRepository, storageService, guestIpQuotaRepository, 24);
  }

  @Test
  void deletesExpiredGuestQuestionSetsAndTheirAudioFiles() {
    UUID questionSetId = UUID.randomUUID();
    QuestionSet staleQuestionSet = QuestionSet.builder().id(questionSetId).build();
    List<QuestionSet> staleQuestionSets = List.of(staleQuestionSet);
    when(questionSetRepository.findStaleGuestQuestionSets(any())).thenReturn(staleQuestionSets);

    AudioSegment segment1 =
        AudioSegment.builder().id(UUID.randomUUID()).s3Key("audio/1.wav").build();
    AudioSegment segment2 =
        AudioSegment.builder().id(UUID.randomUUID()).s3Key("audio/2.wav").build();
    when(audioSegmentRepository.findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(
            questionSetId))
        .thenReturn(List.of(segment1, segment2));

    service().cleanUpExpiredGuestData();

    verify(storageService).delete("audio/1.wav");
    verify(storageService).delete("audio/2.wav");
    verify(questionSetRepository).deleteAll(staleQuestionSets);
    verify(guestIpQuotaRepository).deleteByUsageDateBefore(any());
  }

  @Test
  void doesNothingWhenNoStaleQuestionSetsExist() {
    when(questionSetRepository.findStaleGuestQuestionSets(any())).thenReturn(List.of());

    service().cleanUpExpiredGuestData();

    verify(questionSetRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyIterable());
    verify(guestIpQuotaRepository, never()).deleteByUsageDateBefore(any());
  }

  @Test
  void continuesDeletingRemainingFilesWhenOneStorageDeleteFails() {
    UUID questionSetId = UUID.randomUUID();
    QuestionSet staleQuestionSet = QuestionSet.builder().id(questionSetId).build();
    when(questionSetRepository.findStaleGuestQuestionSets(any()))
        .thenReturn(List.of(staleQuestionSet));

    AudioSegment failingSegment =
        AudioSegment.builder().id(UUID.randomUUID()).s3Key("audio/broken.wav").build();
    AudioSegment okSegment =
        AudioSegment.builder().id(UUID.randomUUID()).s3Key("audio/ok.wav").build();
    when(audioSegmentRepository.findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(
            questionSetId))
        .thenReturn(List.of(failingSegment, okSegment));
    org.mockito.Mockito.doThrow(new RuntimeException("storage unavailable"))
        .when(storageService)
        .delete("audio/broken.wav");

    service().cleanUpExpiredGuestData();

    verify(storageService).delete("audio/ok.wav");
    verify(questionSetRepository).deleteAll(List.of(staleQuestionSet));
  }
}

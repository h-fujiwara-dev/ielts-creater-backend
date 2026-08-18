package com.ieltscreator.api.guest;

import com.ieltscreator.api.questionset.AudioSegment;
import com.ieltscreator.api.questionset.AudioSegmentRepository;
import com.ieltscreator.api.questionset.QuestionSet;
import com.ieltscreator.api.questionset.QuestionSetRepository;
import com.ieltscreator.api.questionset.listening.StorageService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ゲスト（#00056）が生成したデータを定期的に削除するバッチ。共有デモアカウントの{@code question_set}を
 * retention-hoursより古いものから削除する（子テーブルはV4migrationのON DELETE CASCADEで一括削除される）。 ECS Serviceの{@code
 * desired_count}が現状1のため、複数タスク同時稼働時の重複実行制御は本チケットの スコープ外とする。
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "cognito")
public class GuestDataCleanupService {

  private final QuestionSetRepository questionSetRepository;
  private final AudioSegmentRepository audioSegmentRepository;
  private final StorageService storageService;
  private final GuestIpQuotaRepository guestIpQuotaRepository;
  private final int retentionHours;

  public GuestDataCleanupService(
      QuestionSetRepository questionSetRepository,
      AudioSegmentRepository audioSegmentRepository,
      StorageService storageService,
      GuestIpQuotaRepository guestIpQuotaRepository,
      @Value("${app.guest.retention-hours:24}") int retentionHours) {
    this.questionSetRepository = questionSetRepository;
    this.audioSegmentRepository = audioSegmentRepository;
    this.storageService = storageService;
    this.guestIpQuotaRepository = guestIpQuotaRepository;
    this.retentionHours = retentionHours;
  }

  @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
  @Transactional
  public void cleanUpExpiredGuestData() {
    Instant cutoff = Instant.now().minus(retentionHours, ChronoUnit.HOURS);
    List<QuestionSet> staleQuestionSets = questionSetRepository.findStaleGuestQuestionSets(cutoff);
    if (staleQuestionSets.isEmpty()) {
      return;
    }

    for (QuestionSet questionSet : staleQuestionSets) {
      deleteAudioFiles(questionSet.getId());
    }
    questionSetRepository.deleteAll(staleQuestionSets);
    log.info("Deleted {} expired guest question set(s).", staleQuestionSets.size());

    // IPアドレスの保持期間も必要最小限に留める（直近3日分のみ残す）。
    guestIpQuotaRepository.deleteByUsageDateBefore(LocalDate.now().minusDays(3));
  }

  private void deleteAudioFiles(UUID questionSetId) {
    List<AudioSegment> audioSegments =
        audioSegmentRepository.findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(
            questionSetId);
    for (AudioSegment audioSegment : audioSegments) {
      try {
        storageService.delete(audioSegment.getS3Key());
      } catch (RuntimeException e) {
        log.warn("Failed to delete guest audio file for key: {}", audioSegment.getS3Key(), e);
      }
    }
  }
}

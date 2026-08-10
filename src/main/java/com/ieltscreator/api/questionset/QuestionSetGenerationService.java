package com.ieltscreator.api.questionset;

import com.ieltscreator.api.common.exception.RateLimitExceededException;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateRequest;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 問題生成の同期部分（バリデーション・レート制限・エンティティ作成）を担う。実際の生成は{@link
 * QuestionSetGenerationWorker}へ非同期委譲し、リクエストスレッドをブロックしない（実装規約8章）。
 *
 * <p>あえて{@code @Transactional}を付けない: Spring Data JPAのリポジトリ呼び出しは個別にトランザクションを 持つため、{@code
 * save}が返った時点でQuestionSet行はコミット済みになる。もしこのメソッド全体を1つの
 * トランザクションにまとめると、コミット前にワーカースレッドがsubmitされてしまい、ワーカー側の別トランザクションから QuestionSetがまだ見えない競合が発生し得る。
 */
@Service
@RequiredArgsConstructor
public class QuestionSetGenerationService {

  private static final List<String> TOPIC_PRESETS =
      List.of(
          "Environment",
          "Technology",
          "Education",
          "Health",
          "Travel",
          "Culture",
          "Science",
          "Work");
  private static final int DAILY_GENERATION_LIMIT = 2;
  private static final String PROMPT_VERSION = "stub-v1";

  private final QuestionSetRepository questionSetRepository;
  private final QuestionSetGenerationWorker questionSetGenerationWorker;
  private final ExecutorService questionSetGenerationExecutor;

  public QuestionSetCreateResponse startGeneration(UUID userId, QuestionSetCreateRequest request) {
    checkDailyLimit(userId);

    String topic =
        request.topic() == null || request.topic().isBlank()
            ? randomPresetTopic()
            : request.topic();

    QuestionSet questionSet =
        questionSetRepository.save(
            QuestionSet.builder()
                .userId(userId)
                .section(request.section())
                .topic(topic)
                .difficulty(request.difficulty().name())
                .status(QuestionSetStatus.GENERATING)
                .promptVersion(PROMPT_VERSION)
                .build());

    UUID questionSetId = questionSet.getId();
    Section section = request.section();
    Difficulty difficulty = request.difficulty();
    questionSetGenerationExecutor.submit(
        () -> questionSetGenerationWorker.generate(questionSetId, section, topic, difficulty));

    return new QuestionSetCreateResponse(questionSetId, questionSet.getStatus(), topic);
  }

  private void checkDailyLimit(UUID userId) {
    Instant startOfDayUtc = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Instant startOfNextDayUtc = startOfDayUtc.plus(1, ChronoUnit.DAYS);
    long todayCount =
        questionSetRepository.countByUserIdAndCreatedAtBetween(
            userId, startOfDayUtc, startOfNextDayUtc);
    if (todayCount >= DAILY_GENERATION_LIMIT) {
      throw new RateLimitExceededException(
          "Daily question set generation limit (%d) reached.".formatted(DAILY_GENERATION_LIMIT));
    }
  }

  private String randomPresetTopic() {
    return TOPIC_PRESETS.get(ThreadLocalRandom.current().nextInt(TOPIC_PRESETS.size()));
  }
}

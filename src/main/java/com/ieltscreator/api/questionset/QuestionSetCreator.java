package com.ieltscreator.api.questionset;

import com.ieltscreator.api.common.exception.RateLimitExceededException;
import com.ieltscreator.api.user.AppUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザー単位の1日の生成回数チェックとQuestionSet作成を1トランザクションでアトミックに行う。
 *
 * <p>{@link
 * QuestionSetGenerationService#startGeneration}はワーカーへのsubmit前にコミットを確定させるため意図的に{@code @Transactional}を付けていない（同クラスのJavadoc参照）。そのため日次上限のcheck-then-act（count→INSERT）は
 * このクラスの{@code @Transactional}メソッドに切り出し、Postgresのトランザクションスコープアドバイザリロック（{@link
 * QuestionSetRepository#acquireDailyLimitLock}）でユーザー単位に直列化することで、同時リクエストによる
 * 上限すり抜け（レースコンディション）を防ぐ（#00064）。このメソッドの呼び出し完了時点でトランザクションはコミット済みのため、
 * 呼び出し元の「ワーカーは常にコミット済みの行のみを見る」という前提は変わらない。
 */
@Component
@RequiredArgsConstructor
class QuestionSetCreator {

  private static final int DAILY_GENERATION_LIMIT = 2;

  private final QuestionSetRepository questionSetRepository;
  private final AppUserRepository appUserRepository;

  @Transactional
  public QuestionSet createWithinDailyLimit(
      UUID userId, Section section, String topic, Difficulty difficulty, String promptVersion) {
    // 共有デモアカウント（ゲスト、#00056）はユーザーID単位のこの上限を適用しない。
    // 全訪問者で1つのuser_idを共有するため、代わりにGuestQuotaInterceptorがIPアドレス単位で制限する。
    if (!appUserRepository.existsByIdAndIsGuestTrue(userId)) {
      questionSetRepository.acquireDailyLimitLock(userId);
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

    return questionSetRepository.save(
        QuestionSet.builder()
            .userId(userId)
            .section(section)
            .topic(topic)
            .difficulty(difficulty.name())
            .status(QuestionSetStatus.GENERATING)
            .promptVersion(promptVersion)
            .build());
  }
}

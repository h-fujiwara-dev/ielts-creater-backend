package com.ieltscreator.api.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.ieltscreator.api.dashboard.dto.DashboardSummaryResponse;
import com.ieltscreator.api.questionset.QuestionFormatType;
import com.ieltscreator.api.questionset.Section;
import com.ieltscreator.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * READING区分の集計値は、同じ固定devユーザーを共有するAttemptApiIntegrationTest等が提出する他の
 * Attemptの影響を受けうる（結合テスト全体でPostgresコンテナ・DBを共有するため）。そのため、他の
 * どの結合テストも触れないLISTENING区分では厳密な値を検証し、複数区分にまたがる集計はALL/30D期間の 差分など、他テストの有無に依存しない相対比較で検証する。
 */
@Sql("/sql/dashboard-fixture.sql")
class DashboardApiIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void summarizesAcrossSectionsAndFormatsAndAppliesPeriodAndSectionFilters() {
    String listeningUrl =
        UriComponentsBuilder.fromPath("/api/v1/dashboard/summary")
            .queryParam("section", "LISTENING")
            .toUriString();
    DashboardSummaryResponse listeningOnly =
        restTemplate.getForEntity(listeningUrl, DashboardSummaryResponse.class).getBody();
    assertThat(listeningOnly.totalAttempts()).isEqualTo(1);
    assertThat(listeningOnly.averageAccuracyBySection()).containsOnlyKeys(Section.LISTENING);
    assertThat(listeningOnly.averageAccuracyBySection().get(Section.LISTENING))
        .isCloseTo(1.0, within(0.0001));
    assertThat(listeningOnly.accuracyByFormat())
        .containsOnlyKeys(QuestionFormatType.FORM_COMPLETION);
    assertThat(listeningOnly.accuracyByFormat().get(QuestionFormatType.FORM_COMPLETION))
        .isCloseTo(1.0, within(0.0001));

    DashboardSummaryResponse allTime =
        restTemplate
            .getForEntity("/api/v1/dashboard/summary", DashboardSummaryResponse.class)
            .getBody();
    assertThat(allTime.accuracyByFormat())
        .containsKeys(
            QuestionFormatType.TFNG,
            QuestionFormatType.MCQ,
            QuestionFormatType.FILL_BLANK,
            QuestionFormatType.FORM_COMPLETION);
    assertThat(allTime.averageAccuracyBySection()).containsKeys(Section.READING, Section.LISTENING);
    assertThat(allTime.scoreTrend()).isNotEmpty();

    String periodUrl =
        UriComponentsBuilder.fromPath("/api/v1/dashboard/summary")
            .queryParam("period", "30D")
            .toUriString();
    DashboardSummaryResponse last30Days =
        restTemplate.getForEntity(periodUrl, DashboardSummaryResponse.class).getBody();
    // 30D窓の外にあるのはフィクスチャの40日前のAttemptだけ。他の結合テストが同じdevユーザーで
    // 追加提出するAttemptは常に「今」提出されるためALL/30Dの両方に等しく含まれ、差分には影響しない。
    assertThat(allTime.totalAttempts() - last30Days.totalAttempts()).isEqualTo(1);

    String invalidPeriodUrl =
        UriComponentsBuilder.fromPath("/api/v1/dashboard/summary")
            .queryParam("period", "INVALID")
            .toUriString();
    ResponseEntity<String> invalidPeriodResponse =
        restTemplate.getForEntity(invalidPeriodUrl, String.class);
    assertThat(invalidPeriodResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}

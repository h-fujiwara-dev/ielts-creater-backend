package com.ieltscreator.api.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.ieltscreator.api.common.exception.ValidationException;
import com.ieltscreator.api.dashboard.dto.AttemptScoreRow;
import com.ieltscreator.api.dashboard.dto.DashboardSummaryResponse;
import com.ieltscreator.api.dashboard.dto.FormatAccuracyRow;
import com.ieltscreator.api.questionset.QuestionFormatType;
import com.ieltscreator.api.questionset.Section;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {

  @Mock private DashboardQueryRepository dashboardQueryRepository;

  private DashboardSummaryService service() {
    return new DashboardSummaryService(dashboardQueryRepository);
  }

  @Test
  void aggregatesAccuracyBySectionScoreTrendAndFormat() {
    UUID userId = UUID.randomUUID();
    Instant day1 = Instant.parse("2026-08-01T00:00:00Z");
    Instant day2 = Instant.parse("2026-08-02T00:00:00Z");
    when(dashboardQueryRepository.findAttemptScores(userId, null, Instant.EPOCH))
        .thenReturn(
            List.of(
                new AttemptScoreRow(Section.READING, day1, 3, 4),
                new AttemptScoreRow(Section.READING, day2, 4, 4),
                new AttemptScoreRow(Section.LISTENING, day2, 2, 4)));
    when(dashboardQueryRepository.findFormatAccuracy(userId, null, Instant.EPOCH))
        .thenReturn(
            List.of(
                new FormatAccuracyRow(QuestionFormatType.TFNG, true),
                new FormatAccuracyRow(QuestionFormatType.TFNG, false),
                new FormatAccuracyRow(QuestionFormatType.MCQ, true)));

    DashboardSummaryResponse response = service().getSummary(userId, "ALL", null);

    assertThat(response.totalAttempts()).isEqualTo(3);
    assertThat(response.averageAccuracyBySection().get(Section.READING))
        .isCloseTo(0.875, within(0.0001));
    assertThat(response.averageAccuracyBySection().get(Section.LISTENING))
        .isCloseTo(0.5, within(0.0001));
    assertThat(response.accuracyByFormat().get(QuestionFormatType.TFNG))
        .isCloseTo(0.5, within(0.0001));
    assertThat(response.accuracyByFormat().get(QuestionFormatType.MCQ))
        .isCloseTo(1.0, within(0.0001));
    assertThat(response.scoreTrend())
        .containsExactly(
            new DashboardSummaryResponse.ScoreTrendPoint(
                day1.atZone(ZoneOffset.UTC).toLocalDate(), 0.75),
            new DashboardSummaryResponse.ScoreTrendPoint(
                day2.atZone(ZoneOffset.UTC).toLocalDate(), 0.75));
  }

  @Test
  void excludesRowsWithoutAMaxScoreFromAveraging() {
    UUID userId = UUID.randomUUID();
    when(dashboardQueryRepository.findAttemptScores(userId, null, Instant.EPOCH))
        .thenReturn(List.of(new AttemptScoreRow(Section.READING, Instant.now(), null, null)));
    when(dashboardQueryRepository.findFormatAccuracy(userId, null, Instant.EPOCH))
        .thenReturn(List.of());

    DashboardSummaryResponse response = service().getSummary(userId, "ALL", null);

    assertThat(response.totalAttempts()).isEqualTo(1);
    assertThat(response.averageAccuracyBySection()).isEmpty();
    assertThat(response.scoreTrend()).isEmpty();
  }

  @Test
  void rejectsInvalidPeriod() {
    assertThatThrownBy(() -> service().getSummary(UUID.randomUUID(), "INVALID", null))
        .isInstanceOf(ValidationException.class);
  }
}

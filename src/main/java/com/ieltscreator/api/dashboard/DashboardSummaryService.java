package com.ieltscreator.api.dashboard;

import com.ieltscreator.api.dashboard.dto.AttemptScoreRow;
import com.ieltscreator.api.dashboard.dto.DashboardSummaryResponse;
import com.ieltscreator.api.dashboard.dto.FormatAccuracyRow;
import com.ieltscreator.api.questionset.QuestionFormatType;
import com.ieltscreator.api.questionset.Section;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardSummaryService {

  private final DashboardQueryRepository dashboardQueryRepository;

  @Transactional(readOnly = true)
  public DashboardSummaryResponse getSummary(UUID userId, String period, Section section) {
    Instant submittedAfter = DashboardPeriod.parse(period).submittedAfter();

    List<AttemptScoreRow> scoreRows =
        dashboardQueryRepository.findAttemptScores(userId, section, submittedAfter);
    List<FormatAccuracyRow> formatRows =
        dashboardQueryRepository.findFormatAccuracy(userId, section, submittedAfter);

    return new DashboardSummaryResponse(
        scoreRows.size(),
        averageAccuracyBySection(scoreRows),
        scoreTrend(scoreRows),
        accuracyByFormat(formatRows));
  }

  private static Map<Section, Double> averageAccuracyBySection(List<AttemptScoreRow> rows) {
    return rows.stream()
        .filter(DashboardSummaryService::hasScore)
        .collect(
            Collectors.groupingBy(
                AttemptScoreRow::section,
                Collectors.averagingDouble(DashboardSummaryService::accuracy)));
  }

  private static List<DashboardSummaryResponse.ScoreTrendPoint> scoreTrend(
      List<AttemptScoreRow> rows) {
    Map<LocalDate, Double> accuracyByDate =
        rows.stream()
            .filter(DashboardSummaryService::hasScore)
            .collect(
                Collectors.groupingBy(
                    row -> row.submittedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    Collectors.averagingDouble(DashboardSummaryService::accuracy)));
    return accuracyByDate.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(
            entry -> new DashboardSummaryResponse.ScoreTrendPoint(entry.getKey(), entry.getValue()))
        .toList();
  }

  private static Map<QuestionFormatType, Double> accuracyByFormat(List<FormatAccuracyRow> rows) {
    return rows.stream()
        .collect(
            Collectors.groupingBy(
                FormatAccuracyRow::formatType,
                Collectors.averagingDouble(
                    row -> Boolean.TRUE.equals(row.isCorrect()) ? 1.0 : 0.0)));
  }

  private static boolean hasScore(AttemptScoreRow row) {
    return row.maxScore() != null && row.maxScore() > 0;
  }

  private static double accuracy(AttemptScoreRow row) {
    return row.rawScore().doubleValue() / row.maxScore();
  }
}

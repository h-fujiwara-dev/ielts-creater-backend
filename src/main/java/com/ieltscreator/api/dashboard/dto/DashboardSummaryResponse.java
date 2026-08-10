package com.ieltscreator.api.dashboard.dto;

import com.ieltscreator.api.questionset.QuestionFormatType;
import com.ieltscreator.api.questionset.Section;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
    int totalAttempts,
    Map<Section, Double> averageAccuracyBySection,
    List<ScoreTrendPoint> scoreTrend,
    Map<QuestionFormatType, Double> accuracyByFormat) {

  public record ScoreTrendPoint(LocalDate date, double accuracy) {}
}

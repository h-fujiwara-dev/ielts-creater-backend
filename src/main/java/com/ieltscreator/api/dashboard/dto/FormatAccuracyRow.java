package com.ieltscreator.api.dashboard.dto;

import com.ieltscreator.api.questionset.QuestionFormatType;

/** {@link com.ieltscreator.api.dashboard.DashboardQueryRepository}の集計用の内部射影。 */
public record FormatAccuracyRow(QuestionFormatType formatType, Boolean isCorrect) {}

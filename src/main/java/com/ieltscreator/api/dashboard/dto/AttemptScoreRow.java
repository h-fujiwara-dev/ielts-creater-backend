package com.ieltscreator.api.dashboard.dto;

import com.ieltscreator.api.questionset.Section;
import java.time.Instant;

/** {@link com.ieltscreator.api.dashboard.DashboardQueryRepository}の集計用の内部射影。 */
public record AttemptScoreRow(
    Section section, Instant submittedAt, Integer rawScore, Integer maxScore) {}

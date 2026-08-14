package com.ieltscreator.api.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.ieltscreator.api.common.exception.ValidationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class DashboardPeriodTest {

  @Test
  void allHasNoLowerBound() {
    assertThat(DashboardPeriod.parse("ALL").submittedAfter()).isEqualTo(Instant.EPOCH);
  }

  @Test
  void sevenDaysLooksBackSevenDays() {
    Instant expected = Instant.now().minus(7, ChronoUnit.DAYS);
    assertThat(DashboardPeriod.parse("7D").submittedAfter())
        .isCloseTo(expected, within(5, ChronoUnit.SECONDS));
  }

  @Test
  void rejectsUnknownValue() {
    assertThatThrownBy(() -> DashboardPeriod.parse("1Y")).isInstanceOf(ValidationException.class);
  }
}

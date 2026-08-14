package com.ieltscreator.api.dashboard;

import com.ieltscreator.api.common.exception.ValidationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

enum DashboardPeriod {
  D7(7),
  D30(30),
  D90(90),
  ALL(null);

  private final Integer days;

  DashboardPeriod(Integer days) {
    this.days = days;
  }

  /**
   * ALLの下限はnullではなく{@link Instant#EPOCH}にする: JPQLの{@code :submittedAfter is null or ...}に
   * nullのInstantを渡すと、Postgres JDBCが最初に評価される{@code is null}分岐だけではパラメータの型を 決定できず"could not determine
   * data type of parameter"エラーになるため。
   */
  Instant submittedAfter() {
    return days == null ? Instant.EPOCH : Instant.now().minus(days, ChronoUnit.DAYS);
  }

  static DashboardPeriod parse(String rawValue) {
    return switch (rawValue) {
      case "7D" -> D7;
      case "30D" -> D30;
      case "90D" -> D90;
      case "ALL" -> ALL;
      default ->
          throw new ValidationException("period must be one of 7D, 30D, 90D, ALL: " + rawValue);
    };
  }
}

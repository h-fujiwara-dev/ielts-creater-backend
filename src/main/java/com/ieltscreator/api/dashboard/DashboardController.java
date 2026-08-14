package com.ieltscreator.api.dashboard;

import com.ieltscreator.api.common.security.CurrentUserProvider;
import com.ieltscreator.api.dashboard.dto.DashboardSummaryResponse;
import com.ieltscreator.api.questionset.Section;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardSummaryService dashboardSummaryService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping("/summary")
  public DashboardSummaryResponse summary(
      @RequestParam(defaultValue = "ALL") String period,
      @RequestParam(required = false) Section section) {
    return dashboardSummaryService.getSummary(currentUserProvider.currentUserId(), period, section);
  }
}

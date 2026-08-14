package com.ieltscreator.api.attempt.dto;

import java.util.List;

public record AttemptHistoryPageResponse(
    List<AttemptHistoryItemResponse> items, int page, int totalPages) {}

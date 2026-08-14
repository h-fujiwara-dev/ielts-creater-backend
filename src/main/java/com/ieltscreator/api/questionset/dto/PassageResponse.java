package com.ieltscreator.api.questionset.dto;

import java.util.List;

public record PassageResponse(String title, List<ParagraphResponse> paragraphs) {}

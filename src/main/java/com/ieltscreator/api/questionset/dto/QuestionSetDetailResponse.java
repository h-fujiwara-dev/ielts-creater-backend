package com.ieltscreator.api.questionset.dto;

import com.ieltscreator.api.questionset.QuestionSetStatus;
import com.ieltscreator.api.questionset.Section;
import java.util.List;
import java.util.UUID;

/**
 * 正解・metadataは含めない。Listeningの場合、台本本文（{@code turns}）は音声で聞かせる設計のため {@code
 * listeningContext}（場面設定の要約）のみを返す。
 */
public record QuestionSetDetailResponse(
    UUID id,
    Section section,
    String topic,
    String difficulty,
    QuestionSetStatus status,
    PassageResponse passage,
    String listeningContext,
    List<QuestionGroupResponse> questionGroups) {}

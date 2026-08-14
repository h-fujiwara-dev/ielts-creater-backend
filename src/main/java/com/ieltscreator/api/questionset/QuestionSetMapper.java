package com.ieltscreator.api.questionset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.questionset.dto.AnswerOptionResponse;
import com.ieltscreator.api.questionset.dto.AudioSegmentItemResponse;
import com.ieltscreator.api.questionset.dto.AudioSegmentsResponse;
import com.ieltscreator.api.questionset.dto.ParagraphResponse;
import com.ieltscreator.api.questionset.dto.PassageResponse;
import com.ieltscreator.api.questionset.dto.QuestionGroupResponse;
import com.ieltscreator.api.questionset.dto.QuestionResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetDetailResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuestionSetMapper {

  private final ObjectMapper objectMapper;

  QuestionSetDetailResponse toDetailResponse(
      QuestionSet questionSet,
      Passage passage,
      ListeningScript listeningScript,
      List<QuestionGroup> groups,
      Map<UUID, List<Question>> questionsByGroupId,
      Map<UUID, List<AnswerOption>> optionsByQuestionId) {
    List<QuestionGroupResponse> questionGroups =
        groups.stream()
            .map(group -> toQuestionGroupResponse(group, questionsByGroupId, optionsByQuestionId))
            .toList();

    return new QuestionSetDetailResponse(
        questionSet.getId(),
        questionSet.getSection(),
        questionSet.getTopic(),
        questionSet.getDifficulty(),
        questionSet.getStatus(),
        passage == null ? null : toPassageResponse(passage),
        listeningScript == null ? null : listeningScript.getContextText(),
        questionGroups);
  }

  AudioSegmentsResponse toAudioSegmentsResponse(UUID questionSetId, List<AudioSegment> segments) {
    List<AudioSegmentItemResponse> items =
        segments.stream()
            .map(
                segment ->
                    new AudioSegmentItemResponse(
                        segment.getTurnIndex(),
                        "/api/v1/question-sets/%s/audio-segments/%s/file"
                            .formatted(questionSetId, segment.getId()),
                        segment.getDurationMs()))
            .toList();
    return new AudioSegmentsResponse(items);
  }

  @SneakyThrows
  private PassageResponse toPassageResponse(Passage passage) {
    BodyJsonPayload payload = objectMapper.readValue(passage.getBodyJson(), BodyJsonPayload.class);
    return new PassageResponse(passage.getTitle(), payload.paragraphs());
  }

  private QuestionGroupResponse toQuestionGroupResponse(
      QuestionGroup group,
      Map<UUID, List<Question>> questionsByGroupId,
      Map<UUID, List<AnswerOption>> optionsByQuestionId) {
    List<QuestionResponse> questions =
        questionsByGroupId.getOrDefault(group.getId(), List.of()).stream()
            .sorted(Comparator.comparing(Question::getDisplayOrder))
            .map(question -> toQuestionResponse(question, optionsByQuestionId))
            .toList();
    return new QuestionGroupResponse(group.getFormatType(), group.getInstructions(), questions);
  }

  private QuestionResponse toQuestionResponse(
      Question question, Map<UUID, List<AnswerOption>> optionsByQuestionId) {
    List<AnswerOptionResponse> options =
        optionsByQuestionId.getOrDefault(question.getId(), List.of()).stream()
            .map(
                option -> new AnswerOptionResponse(option.getOptionLabel(), option.getOptionText()))
            .toList();
    return new QuestionResponse(
        question.getId(), question.getPromptText(), question.getDisplayOrder(), options);
  }

  private record BodyJsonPayload(List<ParagraphResponse> paragraphs) {}
}

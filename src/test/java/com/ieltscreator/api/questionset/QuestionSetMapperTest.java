package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.questionset.dto.AnswerOptionResponse;
import com.ieltscreator.api.questionset.dto.AudioSegmentsResponse;
import com.ieltscreator.api.questionset.dto.QuestionGroupResponse;
import com.ieltscreator.api.questionset.dto.QuestionResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetDetailResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionSetMapperTest {

  private final QuestionSetMapper mapper = new QuestionSetMapper(new ObjectMapper());

  @Test
  void mapsPassageAndSortsQuestionsByDisplayOrder() {
    UUID questionSetId = UUID.randomUUID();
    QuestionSet questionSet =
        QuestionSet.builder()
            .id(questionSetId)
            .section(Section.READING)
            .topic("Climate")
            .difficulty("MEDIUM")
            .status(QuestionSetStatus.READY)
            .build();
    Passage passage =
        Passage.builder()
            .title("Reading Passage")
            .bodyJson("{\"paragraphs\":[{\"id\":\"p1\",\"text\":\"Hello world\"}]}")
            .build();
    QuestionGroup group =
        QuestionGroup.builder()
            .id(UUID.randomUUID())
            .formatType(QuestionFormatType.TFNG)
            .instructions("Answer true/false/not given")
            .build();
    UUID q1 = UUID.randomUUID();
    UUID q2 = UUID.randomUUID();
    Question secondQuestion =
        Question.builder().id(q2).promptText("Second").displayOrder(2).build();
    Question firstQuestion = Question.builder().id(q1).promptText("First").displayOrder(1).build();

    QuestionSetDetailResponse response =
        mapper.toDetailResponse(
            questionSet,
            passage,
            null,
            List.of(group),
            Map.of(group.getId(), List.of(secondQuestion, firstQuestion)),
            Map.of());

    assertThat(response.id()).isEqualTo(questionSetId);
    assertThat(response.passage().title()).isEqualTo("Reading Passage");
    assertThat(response.passage().paragraphs()).hasSize(1);
    assertThat(response.passage().paragraphs().get(0).text()).isEqualTo("Hello world");
    assertThat(response.listeningContext()).isNull();

    QuestionGroupResponse groupResponse = response.questionGroups().get(0);
    assertThat(groupResponse.formatType()).isEqualTo(QuestionFormatType.TFNG);
    assertThat(groupResponse.questions())
        .extracting(QuestionResponse::promptText)
        .containsExactly("First", "Second");
  }

  @Test
  void mapsListeningContextTextInsteadOfPassageWhenReadingIsAbsent() {
    QuestionSet questionSet =
        QuestionSet.builder()
            .id(UUID.randomUUID())
            .section(Section.LISTENING)
            .status(QuestionSetStatus.READY)
            .build();
    ListeningScript listeningScript =
        ListeningScript.builder().contextText("Two friends discuss weekend plans.").build();

    QuestionSetDetailResponse response =
        mapper.toDetailResponse(questionSet, null, listeningScript, List.of(), Map.of(), Map.of());

    assertThat(response.passage()).isNull();
    assertThat(response.listeningContext()).isEqualTo("Two friends discuss weekend plans.");
  }

  @Test
  void mapsAnswerOptionsForQuestionsThatHaveThem() {
    QuestionSet questionSet =
        QuestionSet.builder().id(UUID.randomUUID()).status(QuestionSetStatus.READY).build();
    QuestionGroup group =
        QuestionGroup.builder().id(UUID.randomUUID()).formatType(QuestionFormatType.MCQ).build();
    UUID questionId = UUID.randomUUID();
    Question question =
        Question.builder().id(questionId).promptText("Pick one").displayOrder(1).build();
    AnswerOption option =
        AnswerOption.builder()
            .questionId(questionId)
            .optionLabel("A")
            .optionText("Option A")
            .build();

    QuestionSetDetailResponse response =
        mapper.toDetailResponse(
            questionSet,
            null,
            null,
            List.of(group),
            Map.of(group.getId(), List.of(question)),
            Map.of(questionId, List.of(option)));

    List<AnswerOptionResponse> options =
        response.questionGroups().get(0).questions().get(0).answerOptions();
    assertThat(options).containsExactly(new AnswerOptionResponse("A", "Option A"));
  }

  @Test
  void buildsAudioSegmentFileUrlFromQuestionSetAndSegmentId() {
    UUID questionSetId = UUID.randomUUID();
    UUID segmentId = UUID.randomUUID();
    AudioSegment segment =
        AudioSegment.builder().id(segmentId).turnIndex(0).durationMs(1500).build();

    AudioSegmentsResponse response =
        mapper.toAudioSegmentsResponse(questionSetId, List.of(segment));

    assertThat(response.segments()).hasSize(1);
    assertThat(response.segments().get(0).turnIndex()).isEqualTo(0);
    assertThat(response.segments().get(0).durationMs()).isEqualTo(1500);
    assertThat(response.segments().get(0).url())
        .isEqualTo(
            "/api/v1/question-sets/%s/audio-segments/%s/file".formatted(questionSetId, segmentId));
  }
}

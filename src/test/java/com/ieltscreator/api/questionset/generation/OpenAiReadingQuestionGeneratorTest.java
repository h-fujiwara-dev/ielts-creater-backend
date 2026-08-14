package com.ieltscreator.api.questionset.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.common.config.OpenAiProperties;
import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.QuestionFormatType;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class OpenAiReadingQuestionGeneratorTest {

  private static final String CHAT_COMPLETIONS_URL = "https://api.openai.test/chat/completions";

  private record TestContext(
      OpenAiReadingQuestionGenerator generator, MockRestServiceServer server) {}

  private static TestContext newContext() {
    ObjectMapper objectMapper = new ObjectMapper();
    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    OpenAiProperties properties = new OpenAiProperties();
    properties.setModel("gpt-4o-mini");
    properties.setMaxAttempts(2);
    properties.setRetryBackoff(Duration.ofMillis(1));
    return new TestContext(
        new OpenAiReadingQuestionGenerator(restClient, properties, objectMapper), server);
  }

  private static String chatCompletionFixture(ObjectMapper objectMapper) throws Exception {
    ReadingSchemaDto dto =
        new ReadingSchemaDto(
            "Renewable Energy: An Overview",
            "Paragraph A text about the topic.",
            "Paragraph B text about the topic.",
            "Paragraph C text about the topic.",
            "Paragraph D text about the topic.",
            new ReadingSchemaDto.TfngQuestionDto("Statement 1", "TRUE", "explanation 1"),
            new ReadingSchemaDto.TfngQuestionDto("Statement 2", "FALSE", "explanation 2"),
            new ReadingSchemaDto.McqQuestionDto(
                "MCQ prompt",
                "Option A",
                "Option B",
                "Option C",
                "Option D",
                "B",
                "mcq explanation"),
            new ReadingSchemaDto.FillBlankQuestionDto(
                "Fill blank prompt", "answer", List.of("answer"), "fill explanation"),
            "Heading I",
            "Heading II",
            "Heading III",
            "Heading IV",
            new ReadingSchemaDto.MatchingHeadingQuestionDto("iii", "match A"),
            new ReadingSchemaDto.MatchingHeadingQuestionDto("ii", "match B"),
            new ReadingSchemaDto.MatchingHeadingQuestionDto("iv", "match C"),
            new ReadingSchemaDto.MatchingHeadingQuestionDto("i", "match D"));
    String innerJson = objectMapper.writeValueAsString(dto);
    String envelope =
        """
        {"choices":[{"message":{"role":"assistant","content":%s}}]}
        """
            .formatted(objectMapper.writeValueAsString(innerJson));
    return envelope;
  }

  @Test
  void succeedsOnFirstTry() throws Exception {
    TestContext ctx = newContext();
    String fixture = chatCompletionFixture(new ObjectMapper());
    ctx.server()
        .expect(requestTo(CHAT_COMPLETIONS_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(fixture, MediaType.APPLICATION_JSON));

    GeneratedReadingContent content = ctx.generator().generate("technology", Difficulty.BAND_6_7);

    assertThat(content.passage().title()).isEqualTo("Renewable Energy: An Overview");
    assertThat(content.passage().paragraphs()).hasSize(4);
    assertThat(content.questionGroups())
        .extracting(GeneratedQuestionGroup::formatType)
        .containsExactly(
            QuestionFormatType.TFNG,
            QuestionFormatType.MCQ,
            QuestionFormatType.FILL_BLANK,
            QuestionFormatType.MATCHING_HEADINGS);
    ctx.server().verify();
  }

  @Test
  void retriesOnceAfterTransientServerErrorThenSucceeds() throws Exception {
    TestContext ctx = newContext();
    String fixture = chatCompletionFixture(new ObjectMapper());
    ctx.server().expect(requestTo(CHAT_COMPLETIONS_URL)).andRespond(withServerError());
    ctx.server()
        .expect(requestTo(CHAT_COMPLETIONS_URL))
        .andRespond(withSuccess(fixture, MediaType.APPLICATION_JSON));

    GeneratedReadingContent content = ctx.generator().generate("technology", Difficulty.BAND_6_7);

    assertThat(content.passage().title()).isEqualTo("Renewable Energy: An Overview");
    ctx.server().verify();
  }

  @Test
  void throwsAfterExhaustingRetriesOnRepeatedServerErrors() {
    TestContext ctx = newContext();
    ctx.server().expect(requestTo(CHAT_COMPLETIONS_URL)).andRespond(withServerError());
    ctx.server().expect(requestTo(CHAT_COMPLETIONS_URL)).andRespond(withServerError());

    assertThatThrownBy(() -> ctx.generator().generate("technology", Difficulty.BAND_6_7))
        .isInstanceOf(RestClientException.class);
    ctx.server().verify();
  }

  @Test
  void failsFastWithoutRetryOnClientError() {
    TestContext ctx = newContext();
    ctx.server()
        .expect(requestTo(CHAT_COMPLETIONS_URL))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> ctx.generator().generate("technology", Difficulty.BAND_6_7))
        .isInstanceOf(RestClientException.class);
    ctx.server().verify();
  }
}

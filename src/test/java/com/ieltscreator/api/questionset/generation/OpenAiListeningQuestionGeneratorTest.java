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
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class OpenAiListeningQuestionGeneratorTest {

  private static final String CHAT_COMPLETIONS_URL = "https://api.openai.test/chat/completions";

  private record TestContext(
      OpenAiListeningQuestionGenerator generator, MockRestServiceServer server) {}

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
        new OpenAiListeningQuestionGenerator(restClient, properties, objectMapper), server);
  }

  private static String chatCompletionFixture(ObjectMapper objectMapper) throws Exception {
    ListeningSchemaDto dto =
        new ListeningSchemaDto(
            "A student calls a course advisor about a course.",
            List.of(
                new ListeningSchemaDto.TurnDto("staff", "Good morning, how can I help?"),
                new ListeningSchemaDto.TurnDto("student", "I'd like to ask about the course."),
                new ListeningSchemaDto.TurnDto("staff", "Sure, could I take your phone number?"),
                new ListeningSchemaDto.TurnDto("student", "It's 0797 654321."),
                new ListeningSchemaDto.TurnDto(
                    "staff", "Thanks, we introduced more funding recently."),
                new ListeningSchemaDto.TurnDto("student", "That's good to hear.")),
            new ListeningSchemaDto.FillBlankQuestionDto(
                "Preferred contact time: ______", "morning", List.of("morning"), "explanation 1"),
            new ListeningSchemaDto.FillBlankQuestionDto(
                "Contact phone number: ______",
                "0797 654321",
                List.of("0797 654321"),
                "explanation 2"),
            new ListeningSchemaDto.FillBlankQuestionDto(
                "Main concern raised: ______",
                "rising costs",
                List.of("rising costs"),
                "explanation 3"),
            new ListeningSchemaDto.FillBlankQuestionDto(
                "Action taken: ______", "more funding", List.of("more funding"), "explanation 4"));
    String innerJson = objectMapper.writeValueAsString(dto);
    return """
        {"choices":[{"message":{"role":"assistant","content":%s}}]}
        """
        .formatted(objectMapper.writeValueAsString(innerJson));
  }

  @Test
  void succeedsOnFirstTry() throws Exception {
    TestContext ctx = newContext();
    String fixture = chatCompletionFixture(new ObjectMapper());
    ctx.server()
        .expect(requestTo(CHAT_COMPLETIONS_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(fixture, MediaType.APPLICATION_JSON));

    GeneratedListeningContent content = ctx.generator().generate("study", Difficulty.BAND_6_7);

    assertThat(content.script().speakers()).hasSize(2);
    assertThat(content.script().turns()).hasSize(6);
    assertThat(content.questionGroups()).hasSize(2);
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

    GeneratedListeningContent content = ctx.generator().generate("study", Difficulty.BAND_6_7);

    assertThat(content.script().turns()).hasSize(6);
    ctx.server().verify();
  }

  @Test
  void throwsAfterExhaustingRetriesOnRepeatedServerErrors() {
    TestContext ctx = newContext();
    ctx.server().expect(requestTo(CHAT_COMPLETIONS_URL)).andRespond(withServerError());
    ctx.server().expect(requestTo(CHAT_COMPLETIONS_URL)).andRespond(withServerError());

    assertThatThrownBy(() -> ctx.generator().generate("study", Difficulty.BAND_6_7))
        .isInstanceOf(RestClientException.class);
    ctx.server().verify();
  }

  @Test
  void failsFastWithoutRetryOnClientError() {
    TestContext ctx = newContext();
    ctx.server()
        .expect(requestTo(CHAT_COMPLETIONS_URL))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> ctx.generator().generate("study", Difficulty.BAND_6_7))
        .isInstanceOf(RestClientException.class);
    ctx.server().verify();
  }
}

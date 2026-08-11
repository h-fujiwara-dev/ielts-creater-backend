package com.ieltscreator.api.questionset.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltscreator.api.common.config.OpenAiProperties;
import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.QuestionFormatType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenAI Structured Outputs（{@code gpt-4o-mini}）を呼び出すListening生成器（#00033、実装規約.md R-3確定値）。
 * 話者ID・voiceIdは{@link StubListeningQuestionGenerator}と同じ固定値をコード側で割り当て、
 * モデルには台本本文と設問のみを生成させる。maxWordsは既存stubと同じ難易度別ロジックでこちら側から計算する。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.generation", name = "mode", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiListeningQuestionGenerator implements ListeningQuestionGenerator {

  private static final String SCHEMA_NAME = "listening_question_set";
  private static final String SCHEMA_RESOURCE = "/openai/listening-question-schema.json";
  private static final String SPEAKER_STAFF = "staff";
  private static final String SPEAKER_STUDENT = "student";
  private static final String STAFF_VOICE_ID = "Joanna";
  private static final String STUDENT_VOICE_ID = "Matthew";
  private static final String SYSTEM_PROMPT =
      "You are an IELTS Listening test writer. Generate an original conversation script and"
          + " questions that strictly follow the requested JSON schema. Every question must be"
          + " answerable solely from the conversation you write.";

  private static final JsonNode SCHEMA = loadSchema();

  private final RestClient openAiRestClient;
  private final OpenAiProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public GeneratedListeningContent generate(String topic, Difficulty difficulty) {
    int maxWords = leniencyMaxWords(difficulty);
    ObjectNode requestBody = buildRequestBody(topic, difficulty, maxWords);
    OpenAiChatCompletionResponse response = callChatCompletions(requestBody);
    ListeningSchemaDto dto = parseContent(response);
    return toGeneratedContent(dto, maxWords);
  }

  private ObjectNode buildRequestBody(String topic, Difficulty difficulty, int maxWords) {
    String userPrompt =
        """
        Topic: %s
        Target level: %s

        Write a conversation between a staff member (speaker "staff") and a student (speaker
        "student") about %s, alternating turns, 6-10 turns in total. The conversation must
        contain enough concrete detail (e.g. a preference, a number, a reason, an action taken)
        to answer all 4 questions below.

        Then write 2 form-completion questions and 2 note-completion questions, each with an
        answer NO MORE THAN %d WORDS taken directly from the conversation.
        """
            .formatted(topic, difficultyGuidance(difficulty), topic, maxWords);

    ObjectNode systemMessage = objectMapper.createObjectNode();
    systemMessage.put("role", "system");
    systemMessage.put("content", SYSTEM_PROMPT);
    ObjectNode userMessage = objectMapper.createObjectNode();
    userMessage.put("role", "user");
    userMessage.put("content", userPrompt);

    ObjectNode jsonSchema = objectMapper.createObjectNode();
    jsonSchema.put("name", SCHEMA_NAME);
    jsonSchema.put("strict", true);
    jsonSchema.set("schema", SCHEMA);

    ObjectNode responseFormat = objectMapper.createObjectNode();
    responseFormat.put("type", "json_schema");
    responseFormat.set("json_schema", jsonSchema);

    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", properties.getModel());
    requestBody.set("messages", objectMapper.createArrayNode().add(systemMessage).add(userMessage));
    requestBody.set("response_format", responseFormat);
    return requestBody;
  }

  private OpenAiChatCompletionResponse callChatCompletions(ObjectNode requestBody) {
    int maxAttempts = properties.getMaxAttempts();
    for (int attempt = 1; ; attempt++) {
      long start = System.currentTimeMillis();
      try {
        String rawResponse =
            openAiRestClient
                .post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);
        log.info(
            "OpenAI listening generation succeeded: attempt={}, elapsedMs={}",
            attempt,
            System.currentTimeMillis() - start);
        log.debug("OpenAI listening response body: {}", rawResponse);
        return objectMapper.readValue(rawResponse, OpenAiChatCompletionResponse.class);
      } catch (RestClientException e) {
        boolean retryable = isRetryable(e) && attempt < maxAttempts;
        log.warn(
            "OpenAI listening generation failed: attempt={}/{}, elapsedMs={}, retrying={}",
            attempt,
            maxAttempts,
            System.currentTimeMillis() - start,
            retryable,
            e);
        if (!retryable) {
          throw e;
        }
        sleep(properties.getRetryBackoff());
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to parse OpenAI listening response", e);
      }
    }
  }

  private ListeningSchemaDto parseContent(OpenAiChatCompletionResponse response) {
    String content = response.choices().get(0).message().content();
    try {
      return objectMapper.readValue(content, ListeningSchemaDto.class);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to parse OpenAI listening structured output", e);
    }
  }

  private GeneratedListeningContent toGeneratedContent(ListeningSchemaDto dto, int maxWords) {
    GeneratedListeningScript script =
        new GeneratedListeningScript(
            dto.scriptContextText(),
            List.of(
                new GeneratedSpeaker(SPEAKER_STAFF, "Course Advisor", STAFF_VOICE_ID),
                new GeneratedSpeaker(SPEAKER_STUDENT, "Student", STUDENT_VOICE_ID)),
            dto.turns().stream()
                .map(turn -> new GeneratedTurn(turn.speaker(), turn.text()))
                .toList());

    List<GeneratedQuestionGroup> questionGroups =
        List.of(buildFormCompletionGroup(dto, maxWords), buildNoteCompletionGroup(dto, maxWords));
    return new GeneratedListeningContent(script, questionGroups);
  }

  private GeneratedQuestionGroup buildFormCompletionGroup(ListeningSchemaDto dto, int maxWords) {
    return new GeneratedQuestionGroup(
        QuestionFormatType.FORM_COMPLETION,
        "Complete the form below. Write NO MORE THAN %d WORDS for each answer.".formatted(maxWords),
        List.of(
            toFillBlankQuestion(dto.formCompletionQuestion1(), maxWords),
            toFillBlankQuestion(dto.formCompletionQuestion2(), maxWords)));
  }

  private GeneratedQuestionGroup buildNoteCompletionGroup(ListeningSchemaDto dto, int maxWords) {
    return new GeneratedQuestionGroup(
        QuestionFormatType.NOTE_COMPLETION,
        "Complete the notes below. Write NO MORE THAN %d WORDS for each answer."
            .formatted(maxWords),
        List.of(
            toFillBlankQuestion(dto.noteCompletionQuestion1(), maxWords),
            toFillBlankQuestion(dto.noteCompletionQuestion2(), maxWords)));
  }

  private GeneratedQuestion toFillBlankQuestion(
      ListeningSchemaDto.FillBlankQuestionDto dto, int maxWords) {
    return GeneratedQuestion.fillBlank(
        dto.promptText(),
        maxWords,
        dto.primaryAnswer(),
        dto.acceptableAnswers(),
        dto.explanation());
  }

  private static boolean isRetryable(RestClientException e) {
    if (e instanceof HttpStatusCodeException statusException) {
      return statusException.getStatusCode().is5xxServerError()
          || statusException.getStatusCode().value() == 429;
    }
    return true;
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to retry OpenAI call", e);
    }
  }

  private static String difficultyGuidance(Difficulty difficulty) {
    return switch (difficulty) {
      case BAND_4_5 ->
          "simple vocabulary and short sentences suitable for an IELTS Band 4-5 candidate";
      case BAND_5_6 ->
          "everyday vocabulary and moderately complex sentences suitable for an IELTS"
              + " Band 5-6 candidate";
      case BAND_6_7 ->
          "varied vocabulary and complex sentence structures suitable for an IELTS"
              + " Band 6-7 candidate";
      case BAND_7_8_PLUS ->
          "advanced academic vocabulary and sophisticated sentence structures"
              + " suitable for an IELTS Band 7-8+ candidate";
    };
  }

  private static int leniencyMaxWords(Difficulty difficulty) {
    return switch (difficulty) {
      case BAND_4_5, BAND_5_6 -> 3;
      case BAND_6_7, BAND_7_8_PLUS -> 2;
    };
  }

  private static JsonNode loadSchema() {
    try (InputStream in =
        OpenAiListeningQuestionGenerator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Missing classpath resource: " + SCHEMA_RESOURCE);
      }
      return new ObjectMapper().readTree(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load " + SCHEMA_RESOURCE, e);
    }
  }
}

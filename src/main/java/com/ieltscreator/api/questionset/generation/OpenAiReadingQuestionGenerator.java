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
 * OpenAI Structured Outputs（{@code gpt-4o-mini}）を呼び出すReading生成器（#00033、実装規約.md R-3確定値）。
 * Passage段落・見出しラベル・MCQラベル等の固定形状は{@link StubReadingQuestionGenerator}と揃え、 {@code
 * correctLabel}系フィールドはJSON Schemaのenumで選択肢と同じラベル集合に縛ることで、 {@link
 * GenerationRuleValidator}が検知する参照整合性違反を構造的に防ぐ。maxWordsはモデルに生成させず、 既存stubと同じ難易度別ロジックでこちら側から計算する。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.generation", name = "mode", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiReadingQuestionGenerator implements ReadingQuestionGenerator {

  private static final String SCHEMA_NAME = "reading_question_set";
  private static final String SCHEMA_RESOURCE = "/openai/reading-question-schema.json";
  private static final String SYSTEM_PROMPT =
      "You are an IELTS Academic Reading test writer. Generate an original passage and questions"
          + " that strictly follow the requested JSON schema. Every question must be answerable"
          + " solely from the passage you write.";

  private static final JsonNode SCHEMA = loadSchema();

  private final RestClient openAiRestClient;
  private final OpenAiProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public GeneratedReadingContent generate(String topic, Difficulty difficulty) {
    int maxWords = leniencyMaxWords(difficulty);
    ObjectNode requestBody = buildRequestBody(topic, difficulty, maxWords);
    OpenAiChatCompletionResponse response = callChatCompletions(requestBody);
    ReadingSchemaDto dto = parseContent(response);
    return toGeneratedContent(dto, maxWords);
  }

  private ObjectNode buildRequestBody(String topic, Difficulty difficulty, int maxWords) {
    String userPrompt =
        """
        Topic: %s
        Target level: %s

        Write a passage with exactly 4 paragraphs (A-D), around 250-300 words in total.
        Then write:
        - 2 True/False/Not Given statements based on the passage
        - 1 multiple-choice question with exactly 4 options
        - 1 fill-in-the-blank sentence whose answer is NO MORE THAN %d WORDS taken from the passage
        - 4 paragraph-heading matches, with exactly 4 distinct heading options (one per paragraph)
        """
            .formatted(topic, difficultyGuidance(difficulty), maxWords);

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
            "OpenAI reading generation succeeded: attempt={}, elapsedMs={}",
            attempt,
            System.currentTimeMillis() - start);
        log.debug("OpenAI reading response body: {}", rawResponse);
        return objectMapper.readValue(rawResponse, OpenAiChatCompletionResponse.class);
      } catch (RestClientException e) {
        boolean retryable = isRetryable(e) && attempt < maxAttempts;
        log.warn(
            "OpenAI reading generation failed: attempt={}/{}, elapsedMs={}, retrying={}",
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
        throw new UncheckedIOException("Failed to parse OpenAI reading response", e);
      }
    }
  }

  private ReadingSchemaDto parseContent(OpenAiChatCompletionResponse response) {
    String content = response.choices().get(0).message().content();
    try {
      return objectMapper.readValue(content, ReadingSchemaDto.class);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to parse OpenAI reading structured output", e);
    }
  }

  private GeneratedReadingContent toGeneratedContent(ReadingSchemaDto dto, int maxWords) {
    GeneratedPassage passage =
        new GeneratedPassage(
            dto.passageTitle(),
            List.of(
                new GeneratedParagraph("A", dto.paragraphA()),
                new GeneratedParagraph("B", dto.paragraphB()),
                new GeneratedParagraph("C", dto.paragraphC()),
                new GeneratedParagraph("D", dto.paragraphD())));

    List<GeneratedQuestionGroup> questionGroups =
        List.of(
            buildTfngGroup(dto),
            buildMcqGroup(dto),
            buildFillBlankGroup(dto, maxWords),
            buildMatchingHeadingsGroup(dto));
    return new GeneratedReadingContent(passage, questionGroups);
  }

  private GeneratedQuestionGroup buildTfngGroup(ReadingSchemaDto dto) {
    return new GeneratedQuestionGroup(
        QuestionFormatType.TFNG,
        "Do the following statements agree with the information given? Write TRUE, FALSE, or NOT"
            + " GIVEN.",
        List.of(
            GeneratedQuestion.tfng(
                dto.tfngStatement1().promptText(),
                dto.tfngStatement1().correctAnswer(),
                dto.tfngStatement1().explanation()),
            GeneratedQuestion.tfng(
                dto.tfngStatement2().promptText(),
                dto.tfngStatement2().correctAnswer(),
                dto.tfngStatement2().explanation())));
  }

  private GeneratedQuestionGroup buildMcqGroup(ReadingSchemaDto dto) {
    ReadingSchemaDto.McqQuestionDto mcq = dto.mcqQuestion();
    List<GeneratedAnswerOption> options =
        List.of(
            new GeneratedAnswerOption("A", mcq.optionA()),
            new GeneratedAnswerOption("B", mcq.optionB()),
            new GeneratedAnswerOption("C", mcq.optionC()),
            new GeneratedAnswerOption("D", mcq.optionD()));
    return new GeneratedQuestionGroup(
        QuestionFormatType.MCQ,
        "Choose the correct letter, A, B, C or D.",
        List.of(
            GeneratedQuestion.mcq(
                mcq.promptText(), options, List.of(mcq.correctLabel()), mcq.explanation())));
  }

  private GeneratedQuestionGroup buildFillBlankGroup(ReadingSchemaDto dto, int maxWords) {
    ReadingSchemaDto.FillBlankQuestionDto fillBlank = dto.fillBlankQuestion();
    return new GeneratedQuestionGroup(
        QuestionFormatType.FILL_BLANK,
        "Complete the sentence below using NO MORE THAN %d WORDS.".formatted(maxWords),
        List.of(
            GeneratedQuestion.fillBlank(
                fillBlank.promptText(),
                maxWords,
                fillBlank.primaryAnswer(),
                fillBlank.acceptableAnswers(),
                fillBlank.explanation())));
  }

  private GeneratedQuestionGroup buildMatchingHeadingsGroup(ReadingSchemaDto dto) {
    List<GeneratedAnswerOption> headingOptions =
        List.of(
            new GeneratedAnswerOption("i", dto.headingOptionI()),
            new GeneratedAnswerOption("ii", dto.headingOptionII()),
            new GeneratedAnswerOption("iii", dto.headingOptionIII()),
            new GeneratedAnswerOption("iv", dto.headingOptionIV()));
    List<GeneratedQuestion> questions =
        List.of(
            GeneratedQuestion.matchingHeading(
                "Paragraph A",
                "A",
                headingOptions,
                dto.matchingHeadingA().correctHeadingLabel(),
                dto.matchingHeadingA().explanation()),
            GeneratedQuestion.matchingHeading(
                "Paragraph B",
                "B",
                headingOptions,
                dto.matchingHeadingB().correctHeadingLabel(),
                dto.matchingHeadingB().explanation()),
            GeneratedQuestion.matchingHeading(
                "Paragraph C",
                "C",
                headingOptions,
                dto.matchingHeadingC().correctHeadingLabel(),
                dto.matchingHeadingC().explanation()),
            GeneratedQuestion.matchingHeading(
                "Paragraph D",
                "D",
                headingOptions,
                dto.matchingHeadingD().correctHeadingLabel(),
                dto.matchingHeadingD().explanation()));
    return new GeneratedQuestionGroup(
        QuestionFormatType.MATCHING_HEADINGS,
        "Match each paragraph with the correct heading from the list below.",
        questions);
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
        OpenAiReadingQuestionGenerator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Missing classpath resource: " + SCHEMA_RESOURCE);
      }
      return new ObjectMapper().readTree(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load " + SCHEMA_RESOURCE, e);
    }
  }
}

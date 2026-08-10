package com.ieltscreator.api.questionset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.common.exception.GenerationFailedException;
import com.ieltscreator.api.common.text.AnswerNormalizer;
import com.ieltscreator.api.questionset.generation.GeneratedAnswerOption;
import com.ieltscreator.api.questionset.generation.GeneratedListeningContent;
import com.ieltscreator.api.questionset.generation.GeneratedListeningScript;
import com.ieltscreator.api.questionset.generation.GeneratedPassage;
import com.ieltscreator.api.questionset.generation.GeneratedQuestion;
import com.ieltscreator.api.questionset.generation.GeneratedQuestionGroup;
import com.ieltscreator.api.questionset.generation.GeneratedReadingContent;
import com.ieltscreator.api.questionset.generation.GeneratedSpeaker;
import com.ieltscreator.api.questionset.generation.GeneratedTurn;
import com.ieltscreator.api.questionset.generation.GenerationRuleValidator;
import com.ieltscreator.api.questionset.generation.ListeningQuestionGenerator;
import com.ieltscreator.api.questionset.generation.ReadingQuestionGenerator;
import com.ieltscreator.api.questionset.listening.ListeningAudioSynthesizer;
import com.ieltscreator.api.questionset.listening.StorageService;
import com.ieltscreator.api.questionset.listening.SynthesizedAudio;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 問題生成の非同期実行本体（{@link AsyncGenerationConfig}のExecutorServiceから呼び出される）。
 * ルール違反時は1回だけ再生成し、それでも違反があれば{@code status=FAILED}として終了する（実装規約・#00025方針）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class QuestionSetGenerationWorker {

  private final QuestionSetRepository questionSetRepository;
  private final PassageRepository passageRepository;
  private final ListeningScriptRepository listeningScriptRepository;
  private final QuestionGroupRepository questionGroupRepository;
  private final QuestionRepository questionRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final AcceptableAnswerRepository acceptableAnswerRepository;
  private final AudioSegmentRepository audioSegmentRepository;
  private final ReadingQuestionGenerator readingQuestionGenerator;
  private final ListeningQuestionGenerator listeningQuestionGenerator;
  private final ListeningAudioSynthesizer listeningAudioSynthesizer;
  private final StorageService storageService;
  private final GenerationRuleValidator generationRuleValidator;
  private final AnswerNormalizer answerNormalizer;
  private final ObjectMapper objectMapper;

  @Transactional
  public void generate(UUID questionSetId, Section section, String topic, Difficulty difficulty) {
    QuestionSet questionSet =
        questionSetRepository
            .findById(questionSetId)
            .orElseThrow(
                () -> new IllegalStateException("QuestionSet not found: " + questionSetId));
    try {
      if (section == Section.READING) {
        generateReading(questionSet, topic, difficulty);
      } else {
        generateListening(questionSet, topic, difficulty);
      }
      questionSet.setStatus(QuestionSetStatus.READY);
    } catch (Exception e) {
      log.error("Question set generation failed: questionSetId={}", questionSetId, e);
      questionSet.setStatus(QuestionSetStatus.FAILED);
      questionSet.setGenerationError(e.getMessage());
    }
    questionSetRepository.save(questionSet);
  }

  private void generateReading(QuestionSet questionSet, String topic, Difficulty difficulty) {
    GeneratedReadingContent content =
        generateWithRetry(
            () -> readingQuestionGenerator.generate(topic, difficulty),
            GeneratedReadingContent::questionGroups);
    persistPassage(questionSet.getId(), content.passage());
    persistQuestionGroups(questionSet.getId(), content.questionGroups());
  }

  private void generateListening(QuestionSet questionSet, String topic, Difficulty difficulty) {
    GeneratedListeningContent content =
        generateWithRetry(
            () -> listeningQuestionGenerator.generate(topic, difficulty),
            GeneratedListeningContent::questionGroups);
    persistListeningScript(questionSet.getId(), content.script());
    persistQuestionGroups(questionSet.getId(), content.questionGroups());
  }

  /** ルール違反があれば1回だけ再生成する。再生成後もなお違反があれば{@link GenerationFailedException}とする。 */
  private <T> T generateWithRetry(
      Supplier<T> generate, Function<T, List<GeneratedQuestionGroup>> questionGroupsOf) {
    T content = generate.get();
    List<String> violations = generationRuleValidator.validate(questionGroupsOf.apply(content));
    if (violations.isEmpty()) {
      return content;
    }
    log.warn("Generated content failed rule validation, retrying once: {}", violations);
    T retried = generate.get();
    List<String> retryViolations =
        generationRuleValidator.validate(questionGroupsOf.apply(retried));
    if (!retryViolations.isEmpty()) {
      throw new GenerationFailedException(
          "Generated content failed rule validation after retry: " + retryViolations);
    }
    return retried;
  }

  private void persistPassage(UUID questionSetId, GeneratedPassage passage) {
    Passage entity =
        Passage.builder()
            .questionSetId(questionSetId)
            .title(passage.title())
            .bodyJson(writeJson(Map.of("paragraphs", passage.paragraphs())))
            .build();
    passageRepository.save(entity);
  }

  private void persistListeningScript(UUID questionSetId, GeneratedListeningScript script) {
    Map<String, Object> scriptPayload = new LinkedHashMap<>();
    scriptPayload.put("speakers", script.speakers());
    scriptPayload.put("turns", script.turns());
    ListeningScript entity =
        ListeningScript.builder()
            .questionSetId(questionSetId)
            .contextText(script.contextText())
            .scriptJson(writeJson(scriptPayload))
            .build();
    ListeningScript saved = listeningScriptRepository.save(entity);
    persistAudioSegments(saved, script);
  }

  private void persistAudioSegments(
      ListeningScript listeningScript, GeneratedListeningScript script) {
    Map<String, String> voiceIdBySpeaker =
        script.speakers().stream()
            .collect(Collectors.toMap(GeneratedSpeaker::id, GeneratedSpeaker::voiceId));
    int turnIndex = 0;
    for (GeneratedTurn turn : script.turns()) {
      String voiceId = voiceIdBySpeaker.get(turn.speakerId());
      SynthesizedAudio audio = listeningAudioSynthesizer.synthesize(turn.text(), voiceId);
      String key = "%s/%d.wav".formatted(listeningScript.getQuestionSetId(), turnIndex);
      String storedKey = storageService.save(key, audio.audioBytes());
      audioSegmentRepository.save(
          AudioSegment.builder()
              .listeningScript(listeningScript)
              .turnIndex(turnIndex)
              .s3Key(storedKey)
              .durationMs(audio.durationMs())
              .voiceId(voiceId)
              .build());
      turnIndex++;
    }
  }

  private void persistQuestionGroups(UUID questionSetId, List<GeneratedQuestionGroup> groups) {
    int groupOrder = 1;
    for (GeneratedQuestionGroup group : groups) {
      QuestionGroup groupEntity =
          questionGroupRepository.save(
              QuestionGroup.builder()
                  .questionSetId(questionSetId)
                  .formatType(group.formatType())
                  .instructions(group.instructions())
                  .displayOrder(groupOrder++)
                  .build());
      persistQuestions(groupEntity, group.questions());
    }
  }

  private void persistQuestions(QuestionGroup groupEntity, List<GeneratedQuestion> questions) {
    int questionOrder = 1;
    for (GeneratedQuestion question : questions) {
      Question questionEntity =
          questionRepository.save(
              Question.builder()
                  .questionGroup(groupEntity)
                  .promptText(question.promptText())
                  .displayOrder(questionOrder++)
                  .metadata(
                      question.metadata() == null || question.metadata().isEmpty()
                          ? null
                          : writeJson(question.metadata()))
                  .correctAnswerKey(writeJson(correctAnswerKeyValue(question)))
                  .explanation(question.explanation())
                  .build());
      persistAnswerOptions(questionEntity.getId(), question.answerOptions());
      persistAcceptableAnswers(questionEntity.getId(), question.acceptableAnswers());
    }
  }

  private Object correctAnswerKeyValue(GeneratedQuestion question) {
    return question.correctAnswerLabels() != null && !question.correctAnswerLabels().isEmpty()
        ? question.correctAnswerLabels()
        : question.correctAnswerText();
  }

  private void persistAnswerOptions(UUID questionId, List<GeneratedAnswerOption> options) {
    for (GeneratedAnswerOption option : options) {
      answerOptionRepository.save(
          AnswerOption.builder()
              .questionId(questionId)
              .optionLabel(option.label())
              .optionText(option.text())
              .build());
    }
  }

  private void persistAcceptableAnswers(UUID questionId, List<String> acceptableAnswers) {
    for (String answerText : acceptableAnswers) {
      acceptableAnswerRepository.save(
          AcceptableAnswer.builder()
              .questionId(questionId)
              .answerText(answerText)
              .normalizedText(answerNormalizer.normalize(answerText))
              .build());
    }
  }

  @SneakyThrows
  private String writeJson(Object value) {
    return objectMapper.writeValueAsString(value);
  }
}

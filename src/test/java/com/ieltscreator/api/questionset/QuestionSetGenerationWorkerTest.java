package com.ieltscreator.api.questionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltscreator.api.common.text.AnswerNormalizer;
import com.ieltscreator.api.questionset.generation.GeneratedListeningContent;
import com.ieltscreator.api.questionset.generation.GeneratedListeningScript;
import com.ieltscreator.api.questionset.generation.GeneratedParagraph;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionSetGenerationWorkerTest {

  @Mock private QuestionSetRepository questionSetRepository;
  @Mock private PassageRepository passageRepository;
  @Mock private ListeningScriptRepository listeningScriptRepository;
  @Mock private QuestionGroupRepository questionGroupRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private AnswerOptionRepository answerOptionRepository;
  @Mock private AcceptableAnswerRepository acceptableAnswerRepository;
  @Mock private AudioSegmentRepository audioSegmentRepository;
  @Mock private ReadingQuestionGenerator readingQuestionGenerator;
  @Mock private ListeningQuestionGenerator listeningQuestionGenerator;
  @Mock private ListeningAudioSynthesizer listeningAudioSynthesizer;
  @Mock private StorageService storageService;
  @Mock private GenerationRuleValidator generationRuleValidator;

  private final AnswerNormalizer answerNormalizer = new AnswerNormalizer();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private QuestionSet questionSet;

  @BeforeEach
  void setUp() {
    questionSet =
        QuestionSet.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .section(Section.READING)
            .topic("Topic")
            .difficulty(Difficulty.BAND_6_7.name())
            .status(QuestionSetStatus.GENERATING)
            .promptVersion("stub-v1")
            .build();
    when(questionSetRepository.findById(questionSet.getId())).thenReturn(Optional.of(questionSet));
  }

  /** 永続化まで進むテストでのみ必要になるスタブ（バリデーション失敗のみを検証するテストでは未使用スタブとして弾かれるため分離）。 */
  private void stubPersistenceEchoesInput() {
    when(questionGroupRepository.save(any()))
        .thenAnswer(
            invocation -> {
              QuestionGroup group = invocation.getArgument(0);
              group.setId(UUID.randomUUID());
              return group;
            });
    when(questionRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Question question = invocation.getArgument(0);
              question.setId(UUID.randomUUID());
              return question;
            });
  }

  private QuestionSetGenerationWorker worker() {
    return new QuestionSetGenerationWorker(
        questionSetRepository,
        passageRepository,
        listeningScriptRepository,
        questionGroupRepository,
        questionRepository,
        answerOptionRepository,
        acceptableAnswerRepository,
        audioSegmentRepository,
        readingQuestionGenerator,
        listeningQuestionGenerator,
        listeningAudioSynthesizer,
        storageService,
        generationRuleValidator,
        answerNormalizer,
        objectMapper);
  }

  private static GeneratedReadingContent validReadingContent() {
    GeneratedQuestion question = GeneratedQuestion.tfng("Prompt", "TRUE", "explanation");
    return new GeneratedReadingContent(
        new GeneratedPassage("Title", List.of(new GeneratedParagraph("A", "text"))),
        List.of(
            new GeneratedQuestionGroup(
                QuestionFormatType.TFNG, "instructions", List.of(question))));
  }

  @Test
  void marksReadyAndPersistsContentWhenValidationPassesOnFirstTry() {
    stubPersistenceEchoesInput();
    when(readingQuestionGenerator.generate("Topic", Difficulty.BAND_6_7))
        .thenReturn(validReadingContent());
    when(generationRuleValidator.validate(any())).thenReturn(List.of());

    worker().generate(questionSet.getId(), Section.READING, "Topic", Difficulty.BAND_6_7);

    verify(readingQuestionGenerator, times(1)).generate("Topic", Difficulty.BAND_6_7);
    verify(passageRepository, times(1)).save(any());
    verify(questionGroupRepository, times(1)).save(any());
    verify(questionRepository, times(1)).save(any());
    assertThat(questionSet.getStatus()).isEqualTo(QuestionSetStatus.READY);
    verify(questionSetRepository).save(questionSet);
  }

  @Test
  void retriesOnceWhenValidationFailsThenSucceeds() {
    stubPersistenceEchoesInput();
    when(readingQuestionGenerator.generate("Topic", Difficulty.BAND_6_7))
        .thenReturn(validReadingContent());
    when(generationRuleValidator.validate(any()))
        .thenReturn(List.of("bad heading label"))
        .thenReturn(List.of());

    worker().generate(questionSet.getId(), Section.READING, "Topic", Difficulty.BAND_6_7);

    verify(readingQuestionGenerator, times(2)).generate("Topic", Difficulty.BAND_6_7);
    assertThat(questionSet.getStatus()).isEqualTo(QuestionSetStatus.READY);
  }

  @Test
  void marksFailedWhenValidationFailsTwice() {
    when(readingQuestionGenerator.generate("Topic", Difficulty.BAND_6_7))
        .thenReturn(validReadingContent());
    when(generationRuleValidator.validate(any())).thenReturn(List.of("still invalid"));

    worker().generate(questionSet.getId(), Section.READING, "Topic", Difficulty.BAND_6_7);

    verify(readingQuestionGenerator, times(2)).generate("Topic", Difficulty.BAND_6_7);
    verify(passageRepository, never()).save(any());
    assertThat(questionSet.getStatus()).isEqualTo(QuestionSetStatus.FAILED);
    assertThat(questionSet.getGenerationError()).contains("still invalid");
  }

  @Test
  void persistsAudioSegmentPerTurnForListening() {
    GeneratedListeningScript script =
        new GeneratedListeningScript(
            "context",
            List.of(
                new GeneratedSpeaker("staff", "Staff", "Joanna"),
                new GeneratedSpeaker("student", "Student", "Matthew")),
            List.of(new GeneratedTurn("staff", "Hello"), new GeneratedTurn("student", "Hi")));
    GeneratedQuestion question =
        GeneratedQuestion.fillBlank("Prompt", 2, "answer", List.of("answer"), null);
    GeneratedListeningContent content =
        new GeneratedListeningContent(
            script,
            List.of(
                new GeneratedQuestionGroup(
                    QuestionFormatType.FORM_COMPLETION, "instructions", List.of(question))));

    stubPersistenceEchoesInput();
    when(listeningScriptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(listeningQuestionGenerator.generate("Topic", Difficulty.BAND_6_7)).thenReturn(content);
    when(generationRuleValidator.validate(any())).thenReturn(List.of());
    when(listeningAudioSynthesizer.synthesize(any(String.class), any(String.class)))
        .thenReturn(new SynthesizedAudio(new byte[] {1, 2, 3}, 500));
    when(storageService.save(any(String.class), any(byte[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    worker().generate(questionSet.getId(), Section.LISTENING, "Topic", Difficulty.BAND_6_7);

    verify(listeningAudioSynthesizer, times(2)).synthesize(any(String.class), any(String.class));
    verify(audioSegmentRepository, times(2)).save(any());
    assertThat(questionSet.getStatus()).isEqualTo(QuestionSetStatus.READY);
  }
}

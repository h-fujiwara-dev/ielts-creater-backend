package com.ieltscreator.api.questionset.generation;

import com.ieltscreator.api.questionset.Difficulty;
import com.ieltscreator.api.questionset.QuestionFormatType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OpenAI/Polly連携の実装に差し替えるまでの決定的なListening生成器。台本の穴埋め系正解は、 topicの長さに依存しない固定フレーズにすることで{@link
 * GenerationRuleValidator}のmaxWords検証を安定して満たす。
 */
@Component
@ConditionalOnProperty(
    prefix = "app.generation",
    name = "mode",
    havingValue = "stub",
    matchIfMissing = true)
public class StubListeningQuestionGenerator implements ListeningQuestionGenerator {

  private static final String SPEAKER_STAFF = "staff";
  private static final String SPEAKER_STUDENT = "student";

  @Override
  public GeneratedListeningContent generate(String topic, Difficulty difficulty) {
    GeneratedListeningScript script = buildScript(topic);
    List<GeneratedQuestionGroup> questionGroups =
        List.of(buildFormCompletionGroup(difficulty), buildNoteCompletionGroup(difficulty));
    return new GeneratedListeningContent(script, questionGroups);
  }

  private GeneratedListeningScript buildScript(String topic) {
    List<GeneratedSpeaker> speakers =
        List.of(
            new GeneratedSpeaker(SPEAKER_STAFF, "Course Advisor", "Joanna"),
            new GeneratedSpeaker(SPEAKER_STUDENT, "Student", "Matthew"));
    List<GeneratedTurn> turns =
        List.of(
            new GeneratedTurn(
                SPEAKER_STAFF, "Good morning, how can I help you with your enquiry today?"),
            new GeneratedTurn(
                SPEAKER_STUDENT,
                "Hi, I'd like to find out more about the course covering %s.".formatted(topic)),
            new GeneratedTurn(
                SPEAKER_STAFF,
                "Sure. We usually contact students in the morning, so could I take your phone"
                    + " number?"),
            new GeneratedTurn(SPEAKER_STUDENT, "Yes, it's 0797 654321."),
            new GeneratedTurn(
                SPEAKER_STAFF,
                "Thanks. One thing to note is that recent feedback pointed to rising costs as the"
                    + " main concern, so we introduced more funding to help with that."),
            new GeneratedTurn(
                SPEAKER_STUDENT, "That's good to hear. Thank you for the information."));
    return new GeneratedListeningScript(
        "A student calls a course advisor to ask about a course covering %s.".formatted(topic),
        speakers,
        turns);
  }

  private GeneratedQuestionGroup buildFormCompletionGroup(Difficulty difficulty) {
    int maxWords = leniencyMaxWords(difficulty);
    List<GeneratedQuestion> questions =
        List.of(
            GeneratedQuestion.fillBlank(
                "Preferred contact time: ______",
                maxWords,
                "morning",
                List.of("morning"),
                "The advisor mentions contacting students in the morning."),
            GeneratedQuestion.fillBlank(
                "Contact phone number: ______",
                maxWords,
                "0797 654321",
                List.of("0797 654321", "0797654321"),
                "The student reads out their phone number during the call."));
    return new GeneratedQuestionGroup(
        QuestionFormatType.FORM_COMPLETION,
        "Complete the form below. Write NO MORE THAN %d WORDS for each answer.".formatted(maxWords),
        questions);
  }

  private GeneratedQuestionGroup buildNoteCompletionGroup(Difficulty difficulty) {
    int maxWords = leniencyMaxWords(difficulty);
    List<GeneratedQuestion> questions =
        List.of(
            GeneratedQuestion.fillBlank(
                "Main concern raised in recent feedback: ______",
                maxWords,
                "rising costs",
                List.of("rising costs", "cost increase"),
                "The advisor refers to rising costs as the main concern in recent feedback."),
            GeneratedQuestion.fillBlank(
                "Action taken in response: ______",
                maxWords,
                "more funding",
                List.of("more funding", "additional funding"),
                "The advisor states that more funding was introduced in response."));
    return new GeneratedQuestionGroup(
        QuestionFormatType.NOTE_COMPLETION,
        "Complete the notes below. Write NO MORE THAN %d WORDS for each answer."
            .formatted(maxWords),
        questions);
  }

  private static int leniencyMaxWords(Difficulty difficulty) {
    return switch (difficulty) {
      case BAND_4_5, BAND_5_6 -> 3;
      case BAND_6_7, BAND_7_8_PLUS -> 2;
    };
  }
}

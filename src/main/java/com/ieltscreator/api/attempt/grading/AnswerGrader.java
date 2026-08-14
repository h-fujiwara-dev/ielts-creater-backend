package com.ieltscreator.api.attempt.grading;

import com.ieltscreator.api.questionset.Question;

public interface AnswerGrader {

  boolean isCorrect(Question question, String userAnswerText);
}

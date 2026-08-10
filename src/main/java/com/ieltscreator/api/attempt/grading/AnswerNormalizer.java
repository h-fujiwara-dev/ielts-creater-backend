package com.ieltscreator.api.attempt.grading;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 穴埋め系設問の表記ゆれ吸収（trim・連続空白圧縮・小文字化・末尾句読点除去）。 */
@Component
public class AnswerNormalizer {

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,;:!?]+$");

  public String normalize(String text) {
    if (text == null) {
      return "";
    }
    String collapsed = WHITESPACE.matcher(text.strip()).replaceAll(" ");
    String lowered = collapsed.toLowerCase(Locale.ROOT);
    return TRAILING_PUNCTUATION.matcher(lowered).replaceAll("").strip();
  }
}

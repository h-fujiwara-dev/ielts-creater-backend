package com.ieltscreator.api.questionset.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** OpenAI Chat Completions APIのレスポンス封筒。Structured Outputsの実体は{@code message.content}のJSON文字列。 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChatCompletionResponse(List<Choice> choices) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(Message message) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Message(String content) {}
}

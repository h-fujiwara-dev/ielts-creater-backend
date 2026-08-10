package com.ieltscreator.api.questionset;

import com.ieltscreator.api.common.security.CurrentUserProvider;
import com.ieltscreator.api.questionset.dto.AudioSegmentsResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateRequest;
import com.ieltscreator.api.questionset.dto.QuestionSetCreateResponse;
import com.ieltscreator.api.questionset.dto.QuestionSetDetailResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/question-sets")
@RequiredArgsConstructor
public class QuestionSetController {

  private final QuestionSetGenerationService questionSetGenerationService;
  private final QuestionSetQueryService questionSetQueryService;
  private final CurrentUserProvider currentUserProvider;

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public QuestionSetCreateResponse create(@Valid @RequestBody QuestionSetCreateRequest request) {
    return questionSetGenerationService.startGeneration(
        currentUserProvider.currentUserId(), request);
  }

  @GetMapping("/{id}")
  public QuestionSetDetailResponse getDetail(@PathVariable UUID id) {
    return questionSetQueryService.getDetail(currentUserProvider.currentUserId(), id);
  }

  @GetMapping("/{id}/audio-segments")
  public AudioSegmentsResponse getAudioSegments(@PathVariable UUID id) {
    return questionSetQueryService.getAudioSegments(currentUserProvider.currentUserId(), id);
  }

  /** Phase1（ローカル保存）向けの音声配信エンドポイント。Phase3ではS3署名付きURLへの直接リダイレクトに差し替える想定。 */
  @GetMapping("/{id}/audio-segments/{audioSegmentId}/file")
  public ResponseEntity<byte[]> getAudioSegmentFile(
      @PathVariable UUID id, @PathVariable UUID audioSegmentId) {
    byte[] audio =
        questionSetQueryService.loadAudioFile(
            currentUserProvider.currentUserId(), id, audioSegmentId);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).body(audio);
  }
}

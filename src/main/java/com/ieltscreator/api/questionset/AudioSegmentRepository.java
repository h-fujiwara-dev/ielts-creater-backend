package com.ieltscreator.api.questionset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioSegmentRepository extends JpaRepository<AudioSegment, UUID> {

  List<AudioSegment> findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(UUID questionSetId);

  Optional<AudioSegment> findByIdAndListeningScript_QuestionSetId(UUID id, UUID questionSetId);
}

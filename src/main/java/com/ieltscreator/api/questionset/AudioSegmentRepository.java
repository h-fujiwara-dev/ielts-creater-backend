package com.ieltscreator.api.questionset;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioSegmentRepository extends JpaRepository<AudioSegment, UUID> {

  List<AudioSegment> findAllByListeningScript_QuestionSetIdOrderByTurnIndexAsc(UUID questionSetId);
}

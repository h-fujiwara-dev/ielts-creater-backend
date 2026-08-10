package com.ieltscreator.api.questionset;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageRepository extends JpaRepository<Passage, UUID> {

  Optional<Passage> findByQuestionSetId(UUID questionSetId);
}

package com.ieltscreator.api.questionset;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListeningScriptRepository extends JpaRepository<ListeningScript, UUID> {

  Optional<ListeningScript> findByQuestionSetId(UUID questionSetId);
}

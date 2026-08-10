package com.ieltscreator.api.questionset;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, UUID> {}

package com.ieltscreator.api.attempt.dto;

import com.ieltscreator.api.attempt.AttemptStatus;
import java.util.UUID;

public record AttemptStartResponse(UUID id, AttemptStatus status) {}

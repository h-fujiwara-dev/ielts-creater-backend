package com.ieltscreator.api.user.dto;

import java.util.UUID;

public record MeResponse(UUID id, String email, String displayName, boolean isGuest) {}

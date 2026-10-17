package de.talentmatch.auth.api;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserDto user) {
}

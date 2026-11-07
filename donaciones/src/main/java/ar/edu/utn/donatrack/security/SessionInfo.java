package ar.edu.utn.donatrack.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionInfo(String sessionId, UUID userId, Instant expiresAt, Set<Role> roles) {}

package ar.edu.utn.donatrack.security;

import java.time.Instant;
import java.util.Set;

public record SessionInfo(String sessionId, String userId, Instant expiresAt, Set<Role> roles) {}

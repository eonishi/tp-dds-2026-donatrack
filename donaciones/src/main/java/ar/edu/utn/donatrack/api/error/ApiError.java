package ar.edu.utn.donatrack.api.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    List<String> details) {}

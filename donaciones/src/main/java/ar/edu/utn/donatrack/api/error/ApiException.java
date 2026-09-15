package ar.edu.utn.donatrack.api.error;

import java.util.List;

public class ApiException extends RuntimeException {
  private final int status;
  private final String code;
  private final List<String> details;

  public ApiException(int status, String code, String message) {
    this(status, code, message, List.of());
  }

  public ApiException(int status, String code, String message, List<String> details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = List.copyOf(details);
  }

  public static ApiException badRequest(String message) {
    return new ApiException(400, "BAD_REQUEST", message);
  }

  public static ApiException notFound(String recurso, Object id) {
    return new ApiException(404, "NOT_FOUND", recurso + " " + id + " no existe.");
  }

  public static ApiException conflict(String message) {
    return new ApiException(409, "CONFLICT", message);
  }

  public int getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public List<String> getDetails() {
    return details;
  }
}

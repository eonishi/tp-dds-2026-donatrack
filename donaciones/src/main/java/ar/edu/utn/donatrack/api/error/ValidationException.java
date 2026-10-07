package ar.edu.utn.donatrack.api.error;

import java.util.List;

public class ValidationException extends ApiException {
  public ValidationException(List<String> details) {
    super(400, "VALIDATION_ERROR", "La solicitud contiene datos inválidos.", details);
  }
}

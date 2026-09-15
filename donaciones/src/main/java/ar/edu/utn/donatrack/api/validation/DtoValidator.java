package ar.edu.utn.donatrack.api.validation;

import ar.edu.utn.donatrack.api.error.ValidationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public final class DtoValidator {
  private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  private DtoValidator() {}

  public static <T> T validate(T dto) {
    if (dto == null) {
      throw new ValidationException(java.util.List.of("body: es obligatorio"));
    }
    var errors =
        VALIDATOR.validate(dto).stream()
            .map(error -> error.getPropertyPath() + ": " + error.getMessage())
            .sorted()
            .toList();
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    return dto;
  }
}

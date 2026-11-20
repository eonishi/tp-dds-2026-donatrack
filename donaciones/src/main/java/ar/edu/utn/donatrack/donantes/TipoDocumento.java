package ar.edu.utn.donatrack.donantes;

public enum TipoDocumento {
  DNI,
  CUIT {
    @Override
    void exigirForma(String numero) {
      if (!numero.matches("[0-9]{11}")) {
        throw new IllegalArgumentException("El CUIT debe tener 11 dígitos.");
      }
    }
  };

  /**
   * El número tal como se guarda: sin espacios, puntos ni guiones, así "30-71234567-9" y
   * "30712345679" son el mismo documento venga de donde venga.
   *
   * @throws IllegalArgumentException si falta o no tiene la forma de este tipo de documento
   */
  public String normalizar(String numero) {
    if (numero == null || numero.isBlank()) {
      throw new IllegalArgumentException("Falta el número de documento.");
    }
    String limpio = numero.replaceAll("[\\s.-]", "");
    exigirForma(limpio);
    return limpio;
  }

  void exigirForma(String numero) {}
}

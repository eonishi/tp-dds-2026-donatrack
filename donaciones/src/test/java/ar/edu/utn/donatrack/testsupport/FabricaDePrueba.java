package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fábricas de objetos de dominio para tests.
 *
 * <p>La asignación real de identificadores ocurre en el borde del servicio (persistencia o alta);
 * en tests se resuelve acá: un id nuevo por instancia, sin repetir literales en cada caso. Los
 * tests que necesitan una identidad concreta la construyen explícitamente.
 */
public final class FabricaDePrueba {

  private FabricaDePrueba() {}

  /**
   * Crea una entidad beneficiaria con un id nuevo y datos de contacto genéricos.
   *
   * @param razonSocial razón social de la entidad
   * @return la entidad creada
   */
  public static EntidadBeneficiaria nuevaEntidad(String razonSocial) {
    return nuevaEntidad(razonSocial, "Direccion", "Telefono");
  }

  /**
   * Crea una entidad beneficiaria con un id nuevo.
   *
   * @param razonSocial razón social de la entidad
   * @param direccion dirección de la entidad
   * @param telefono teléfono de contacto de la entidad
   * @return la entidad creada
   */
  public static EntidadBeneficiaria nuevaEntidad(
      String razonSocial, String direccion, String telefono) {
    return new EntidadBeneficiaria(idDeEntidadNuevo(), razonSocial, direccion, telefono);
  }

  /**
   * Crea una donación con un id nuevo.
   *
   * @param donante donante de la donación
   * @param bienes bienes donados
   * @return la donación creada
   */
  public static Donacion nuevaDonacion(Donante donante, List<Bien> bienes) {
    return new Donacion(UUID.randomUUID(), donante, bienes);
  }

  private static Long idDeEntidadNuevo() {
    return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
  }
}

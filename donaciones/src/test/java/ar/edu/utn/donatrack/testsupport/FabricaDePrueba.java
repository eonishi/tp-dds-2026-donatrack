package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import java.util.List;

/**
 * Fábricas de objetos de dominio para tests.
 *
 * <p>Las instancias se construyen igual que en producción: sin id, porque lo asigna el proveedor de
 * persistencia al guardarlas. Los tests que necesitan una identidad concreta antes de persistir
 * usan los constructores con id de cada entidad.
 */
public final class FabricaDePrueba {

  private FabricaDePrueba() {}

  /**
   * Crea una entidad beneficiaria sin id y datos de contacto genéricos.
   *
   * @param razonSocial razón social de la entidad
   * @return la entidad creada
   */
  public static EntidadBeneficiaria nuevaEntidad(String razonSocial) {
    return nuevaEntidad(razonSocial, "Direccion", "Telefono");
  }

  /**
   * Crea una entidad beneficiaria sin id.
   *
   * @param razonSocial razón social de la entidad
   * @param direccion dirección de la entidad
   * @param telefono teléfono de contacto de la entidad
   * @return la entidad creada
   */
  public static EntidadBeneficiaria nuevaEntidad(
      String razonSocial, String direccion, String telefono) {
    return new EntidadBeneficiaria(razonSocial, direccion, telefono);
  }

  /**
   * Crea una donación sin id.
   *
   * @param donante donante de la donación
   * @param bienes bienes donados
   * @return la donación creada
   */
  public static Donacion nuevaDonacion(Donante donante, List<Bien> bienes) {
    return new Donacion(donante, bienes);
  }
}

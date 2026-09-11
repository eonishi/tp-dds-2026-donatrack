package ar.edu.utn.donatrack.beneficiarios;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTest {

  @Test
  void mismoId_SeDeduplicaEnHashSet() {
    EntidadBeneficiariaId id = new EntidadBeneficiariaId(1L);
    EntidadBeneficiaria a =
        new EntidadBeneficiaria(id, "Comedor Los Pibes", "Av. Siempre Viva 123", "011-4567-8901");
    EntidadBeneficiaria b =
        new EntidadBeneficiaria(id, "Otro Nombre", "Calle Falsa 456", "011-1111-2222");

    Set<EntidadBeneficiaria> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(1, set.size());
  }

  @Test
  void distintoId_NoSeDeduplicaEnHashSet() {
    EntidadBeneficiaria a =
        new EntidadBeneficiaria(
            new EntidadBeneficiariaId(1L),
            "Comedor Los Pibes",
            "Av. Siempre Viva 123",
            "011-4567-8901");
    EntidadBeneficiaria b =
        new EntidadBeneficiaria(
            new EntidadBeneficiariaId(2L), "Comedor Los Pibes", "Calle Falsa 456", "011-1111-2222");

    Set<EntidadBeneficiaria> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(2, set.size());
  }

  @Test
  void getId_DevuelveElIdAsignado() {
    EntidadBeneficiariaId id = new EntidadBeneficiariaId(42L);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id, "Test", "Dir", "Tel");

    assertEquals(id, entidad.getId());
  }

  @Test
  void idNull_LanzaNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> new EntidadBeneficiaria(null, "Test", "Dir", "Tel"));
  }

  @Test
  void agregaCorreoElectronico_YLoRecupera() {
    EntidadBeneficiaria eb = nuevaEntidad("Test");
    CorreoElectronico correo = new CorreoElectronico("test@example.com");
    eb.agregarCorreo(correo);

    assertEquals(1, eb.getCorreos().size());
    assertEquals("test@example.com", eb.getCorreos().get(0).getContacto());
  }

  @Test
  void agregaMultiplesCorreos_YLoSRecuperaTodos() {
    EntidadBeneficiaria eb = nuevaEntidad("Test");
    eb.agregarCorreo(new CorreoElectronico("a@example.com"));
    eb.agregarCorreo(new CorreoElectronico("b@example.com"));
    eb.agregarCorreo(new CorreoElectronico("c@example.com"));

    assertEquals(3, eb.getCorreos().size());
    assertEquals("a@example.com", eb.getCorreos().get(0).getContacto());
    assertEquals("b@example.com", eb.getCorreos().get(1).getContacto());
    assertEquals("c@example.com", eb.getCorreos().get(2).getContacto());
  }
}

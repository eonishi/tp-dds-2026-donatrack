package ar.edu.utn.donatrack.beneficiarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTest {

  @Test
  void mismaRazonSocial_SeDeduplicaEnHashSet() {
    EntidadBeneficiaria a =
        new EntidadBeneficiaria("Comedor Los Pibes", "Av. Siempre Viva 123", "011-4567-8901");
    EntidadBeneficiaria b =
        new EntidadBeneficiaria("Comedor Los Pibes", "Calle Falsa 456", "011-1111-2222");

    Set<EntidadBeneficiaria> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(1, set.size());
  }

  @Test
  void distintaRazonSocial_NoSeDeduplicaEnHashSet() {
    EntidadBeneficiaria a =
        new EntidadBeneficiaria("Comedor Los Pibes", "Av. Siempre Viva 123", "011-4567-8901");
    EntidadBeneficiaria b =
        new EntidadBeneficiaria("Hogar de Ninos", "Calle Falsa 456", "011-1111-2222");

    Set<EntidadBeneficiaria> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(2, set.size());
  }

  @Test
  void agregaCorreoElectronico_YLoRecupera() {
    EntidadBeneficiaria eb = new EntidadBeneficiaria("Test", "Direccion 123", "011-1111-2222");
    CorreoElectronico correo = new CorreoElectronico("test@example.com");
    eb.agregarCorreo(correo);

    assertEquals(1, eb.getCorreos().size());
    assertEquals("test@example.com", eb.getCorreos().get(0).getContacto());
  }

  @Test
  void agregaMultiplesCorreos_YLoSRecuperaTodos() {
    EntidadBeneficiaria eb = new EntidadBeneficiaria("Test", "Direccion 123", "011-1111-2222");
    eb.agregarCorreo(new CorreoElectronico("a@example.com"));
    eb.agregarCorreo(new CorreoElectronico("b@example.com"));
    eb.agregarCorreo(new CorreoElectronico("c@example.com"));

    assertEquals(3, eb.getCorreos().size());
    assertEquals("a@example.com", eb.getCorreos().get(0).getContacto());
    assertEquals("b@example.com", eb.getCorreos().get(1).getContacto());
    assertEquals("c@example.com", eb.getCorreos().get(2).getContacto());
  }
}

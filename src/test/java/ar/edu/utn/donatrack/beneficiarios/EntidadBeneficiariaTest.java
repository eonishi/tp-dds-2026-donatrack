package ar.edu.utn.donatrack.beneficiarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

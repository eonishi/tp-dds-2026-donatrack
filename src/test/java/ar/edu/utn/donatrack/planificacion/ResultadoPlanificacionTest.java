package ar.edu.utn.donatrack.planificacion;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.planificacion.LoteId;
import ar.edu.utn.donatrack.logistica.planificacion.ResultadoPlanificacion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResultadoPlanificacionTest {

  @Test
  void crearResultado_ConDatosValidos_DebeTenerAtributosCorrectos() {
    LoteId loteId = LoteId.generar();
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Entidad", "Dir", "Tel");
    Map<Camion, List<EntidadBeneficiaria>> rutas = Map.of(camion, List.of(entidad));

    ResultadoPlanificacion resultado = new ResultadoPlanificacion(loteId, rutas);

    assertSame(loteId, resultado.loteId());
    assertSame(rutas, resultado.rutas());
  }

  @Test
  void crearResultado_ConLoteIdNull_DebeLanzarNullPointerException() {
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Entidad", "Dir", "Tel");
    Map<Camion, List<EntidadBeneficiaria>> rutas = Map.of(camion, List.of(entidad));

    assertThrows(NullPointerException.class, () -> new ResultadoPlanificacion(null, rutas));
  }

  @Test
  void crearResultado_ConRutasNull_DebeLanzarNullPointerException() {
    LoteId loteId = LoteId.generar();
    assertThrows(NullPointerException.class, () -> new ResultadoPlanificacion(loteId, null));
  }

  @Test
  void crearResultado_ConRutasVacias_DebeSerValido() {
    LoteId loteId = LoteId.generar();
    Map<Camion, List<EntidadBeneficiaria>> rutas = Map.of();
    ResultadoPlanificacion resultado = new ResultadoPlanificacion(loteId, rutas);
    assertTrue(resultado.rutas().isEmpty());
  }
}

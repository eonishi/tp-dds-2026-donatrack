package ar.edu.utn.donatrack.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

public class EventoEntregaNoSastifactoriaTest {
    private Donante donanteMock;
    private EntidadBeneficiaria entidadMock;

    @BeforeEach 
    void setUp() {
        donanteMock = mock(Donante.class);
        when(donanteMock.getMedioContactoPredeterminado()).thenReturn(new CorreoElectronico("donante@test.com"));
        entidadMock = mock(EntidadBeneficiaria.class);
        when(entidadMock.getMediosDeContacto()).thenReturn(List.of(new CorreoElectronico("entidad@test.com")));
        when(entidadMock.getRazonSocial()).thenReturn("Razon Test");
    }

    @AfterEach 
    void terDown() {
        System.clearProperty("donatrack.admin.emails");
    }

    @Test 
    void sinConfiguracion_notificarSoloDonanteYEntidad() {
        var evento = new EventoEntregaNoSatisfactoria(donanteMock, entidadMock, "Motivo test");
        assertEquals(2, evento.generarNotificaciones().size());
    }

    @Test 
    void conConfiguracion_notificaTambienAAdmins() {
        System.setProperty("donatrack.admin.emails", "admin1@test.com, admin2@test.com");
        var evento = new EventoEntregaNoSatisfactoria(donanteMock, entidadMock, "Motivo test");
        assertEquals(4, evento.generarNotificaciones().size());
    }
}

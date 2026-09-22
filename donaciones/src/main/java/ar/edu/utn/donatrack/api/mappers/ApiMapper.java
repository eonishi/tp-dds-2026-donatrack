package ar.edu.utn.donatrack.api.mappers;

import ar.edu.utn.donatrack.api.dto.DonacionDtos.BienResponse;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.DonacionResponse;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.HistorialEstadoResponse;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteResponse;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoResponse;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.RepresentanteRequest;
import ar.edu.utn.donatrack.api.dto.EntidadDtos.EntidadResponse;
import ar.edu.utn.donatrack.api.dto.MatchmakingDtos.CandidatoResponse;
import ar.edu.utn.donatrack.api.dto.MatchmakingDtos.PropuestaResponse;
import ar.edu.utn.donatrack.api.dto.MatchmakingDtos.RankingResponse;
import ar.edu.utn.donatrack.api.dto.NecesidadDtos.NecesidadResponse;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.beneficiarios.NecesidadRecurrente;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.donaciones.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.utilidades.TipoPersona;
import java.util.List;

public final class ApiMapper {
  private ApiMapper() {}

  public static DonanteResponse donante(Donante donante) {
    PersonaHumana humana = donante instanceof PersonaHumana valor ? valor : null;
    PersonajeJuridica juridica = donante instanceof PersonajeJuridica valor ? valor : null;
    List<MedioContactoResponse> contactos =
        donante.getMediosDeContacto().stream()
            .map(
                medio ->
                    new MedioContactoResponse(
                        medio instanceof CorreoElectronico
                            ? "EMAIL"
                            : medio instanceof Whatsapp ? "WHATSAPP" : "SMS",
                        medio.getContacto(),
                        medio.equals(donante.getMedioContactoPredeterminado())))
            .toList();
    List<RepresentanteRequest> representantes =
        juridica == null
            ? List.of()
            : juridica.getRepresentantes().stream()
                .map(r -> new RepresentanteRequest(r.getNombre(), r.getApellido()))
                .toList();
    return new DonanteResponse(
        donante.getId(),
        humana != null ? TipoPersona.HUMANA : TipoPersona.JURIDICA,
        donante.getNumDocumento(),
        donante.getDocumento(),
        contactos,
        humana == null ? null : humana.getNombre(),
        humana == null ? null : humana.getApellido(),
        humana == null ? null : humana.getEdad(),
        humana == null ? null : humana.getGenero(),
        humana == null ? null : humana.getDireccion(),
        juridica == null ? null : juridica.getRazonSocial(),
        juridica == null ? null : juridica.getTipoOrganizacion(),
        juridica == null ? null : juridica.getRubro(),
        representantes);
  }

  public static EntidadResponse entidad(EntidadBeneficiaria entidad) {
    return new EntidadResponse(
        entidad.getId().value(),
        entidad.getRazonSocial(),
        entidad.getDireccion(),
        entidad.getTelefono(),
        entidad.getCorreos().stream().map(CorreoElectronico::getContacto).toList(),
        entidad.necesidades().stream().map(Necesidad::getId).toList());
  }

  public static NecesidadResponse necesidad(Necesidad necesidad, EntidadBeneficiaria entidad) {
    NecesidadRecurrente recurrente = necesidad instanceof NecesidadRecurrente valor ? valor : null;
    return new NecesidadResponse(
        necesidad.getId(),
        entidad.getId().value(),
        recurrente == null ? "EXTRAORDINARIA" : "RECURRENTE",
        recurrente == null ? null : recurrente.getPeriodo(),
        necesidad.getDescripcion(),
        necesidad.getCantidadObjetivo(),
        necesidad.getCantidadRecibida(),
        necesidad.estaSatisfecha(),
        necesidad.getUnidad(),
        necesidad.getSubcategoria().getNombre(),
        necesidad.getSubcategoria().getCategoria());
  }

  public static DonacionResponse donacion(Donacion donacion) {
    List<BienResponse> bienes =
        donacion.getBienes().stream()
            .map(
                bien ->
                    new BienResponse(
                        bien.getDescripcion(),
                        bien.getFoto(),
                        bien.getCantidad(),
                        bien.getUnidad(),
                        bien.getFechaVencimiento(),
                        bien.getEstadoDeUso(),
                        bien.getSubcategoria().getNombre(),
                        bien.getSubcategoria().getCategoria()))
            .toList();
    return new DonacionResponse(
        donacion.getId().value(),
        donacion.getDonante().getId(),
        donacion.getEntidadBeneficiaria() == null
            ? null
            : donacion.getEntidadBeneficiaria().getId().value(),
        donacion.getEstado(),
        bienes,
        donacion.getFotos());
  }

  public static HistorialEstadoResponse historial(HistorialEstado historial) {
    return new HistorialEstadoResponse(
        historial.getSecuencia(),
        historial.getFecha(),
        historial.getEstadoAnterior(),
        historial.getEstadoAlcanzado(),
        historial.getJustificacion());
  }

  public static PropuestaResponse propuesta(PropuestaDeAsignacion propuesta) {
    List<RankingResponse> rankings =
        propuesta.getRankings().stream()
            .map(
                ranking ->
                    new RankingResponse(
                        ranking.getAlgoritmo(),
                        ranking.getCandidatos().stream()
                            .map(
                                candidato ->
                                    new CandidatoResponse(
                                        candidato.getEntidadBeneficiaria().getId().value(),
                                        candidato.getEntidadBeneficiaria().getRazonSocial(),
                                        candidato.getPuntaje()))
                            .toList()))
            .toList();
    return new PropuestaResponse(
        propuesta.getDonacion().getId().value(), propuesta.isPendiente(), rankings);
  }
}

package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonantesRepository {
  private final List<Donante> donantes = new ArrayList<>();

  public Optional<Donante> buscarPorEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      return Optional.empty();
    }
    return donantes.stream()
        .filter(d -> email.trim().equalsIgnoreCase(d.getEmail()))
        .findFirst();
  }

  public List<Donante> getDonantes() {
    return donantes;
  }

  public Donante registrarOActualizar(Donante nuevoDonante) {
    String email = nuevoDonante.getEmail();
    Optional<Donante> existenteOpt = buscarPorEmail(email);

    if (existenteOpt.isPresent()) {
      Donante existente = existenteOpt.get();
      // Actualizar campos comunes
      existente.setNumDocumento(nuevoDonante.getNumDocumento());
      existente.setDocumento(nuevoDonante.getDocumento());

      // Agregar nuevos medios de contacto que no existan
      for (MedioDeContacto medio : nuevoDonante.getMediosDeContacto()) {
        existente.agregarMedioDeContacto(medio);
      }
      if (nuevoDonante.getMedioContactoPredeterminado() != null) {
        existente.setMedioContactoPredeterminado(nuevoDonante.getMedioContactoPredeterminado());
      }

      // Actualizar por subtipo
      if (existente instanceof PersonaHumana && nuevoDonante instanceof PersonaHumana) {
        PersonaHumana eHumana = (PersonaHumana) existente;
        PersonaHumana nHumana = (PersonaHumana) nuevoDonante;
        eHumana.setNombre(nHumana.getNombre());
        eHumana.setApellido(nHumana.getApellido());
        eHumana.setEdad(nHumana.getEdad());
        eHumana.setGenero(nHumana.getGenero());
        eHumana.setDireccion(nHumana.getDireccion());
      } else if (existente instanceof PersonajeJuridica && nuevoDonante instanceof PersonajeJuridica) {
        PersonajeJuridica eJuridica = (PersonajeJuridica) existente;
        PersonajeJuridica nJuridica = (PersonajeJuridica) nuevoDonante;
        eJuridica.setRazonSocial(nJuridica.getRazonSocial());
        // setTipoOrganizacion y setRubro si los proveemos, agreguemos setters en PersonajeJuridica
        eJuridica.setTipoOrganizacion(nJuridica.getTipoOrganizacion());
        eJuridica.setRubro(nJuridica.getRubro());
        for (Representante rep : nJuridica.getRepresentantes()) {
          if (!eJuridica.getRepresentantes().contains(rep)) {
            eJuridica.agregarRepresentante(rep);
          }
        }
      }
      return existente;
    } else {
      donantes.add(nuevoDonante);
      return nuevoDonante;
    }
  }

  public void clear() {
    donantes.clear();
  }
}

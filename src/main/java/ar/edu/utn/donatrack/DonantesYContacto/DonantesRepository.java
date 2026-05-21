package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DonantesRepository {
  private final List<Donante> donantes = new ArrayList<>();

  public Optional<Donante> buscarPorEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      return Optional.empty();
    }
    return donantes.stream()
        .filter(d -> d.extraerEmail().isPresent() && d.extraerEmail().get().equalsIgnoreCase(email.trim()))
        .findFirst();
  }

  public List<Donante> getDonantes() {
    return Collections.unmodifiableList(donantes);
  }

  public Donante registrarOActualizar(Donante nuevoDonante) {
    Optional<String> emailOpt = nuevoDonante.extraerEmail();
    if (emailOpt.isEmpty()) {
        donantes.add(nuevoDonante);
        return nuevoDonante;
    }

    Optional<Donante> existenteOpt = buscarPorEmail(emailOpt.get());

    if (existenteOpt.isPresent()) {
      Donante existente = existenteOpt.get();

      for (MedioDeContacto medio : nuevoDonante.getMediosDeContacto()) {
        existente.setMedioDeContacto(medio);
      }

      if (existente instanceof PersonaHumana && nuevoDonante instanceof PersonaHumana) {
        ((PersonaHumana) existente).actualizarDatosDesdeCSV((PersonaHumana) nuevoDonante);
      } else if (existente instanceof PersonajeJuridica && nuevoDonante instanceof PersonajeJuridica) {
        ((PersonajeJuridica) existente).actualizarDatosDesdeCSV((PersonajeJuridica) nuevoDonante);
      }
      return existente;
    } else {
      donantes.add(nuevoDonante);
      return nuevoDonante;
    }
  }
}
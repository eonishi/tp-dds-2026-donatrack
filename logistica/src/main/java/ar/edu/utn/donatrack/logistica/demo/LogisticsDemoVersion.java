package ar.edu.utn.donatrack.logistica.demo;

import javax.persistence.Entity;
import javax.persistence.Id;

/** Committed with demo records; restarts preserve all subsequent user changes. */
@Entity
public class LogisticsDemoVersion {
  @Id private String id;

  protected LogisticsDemoVersion() {}

  LogisticsDemoVersion(String id) {
    this.id = id;
  }
}

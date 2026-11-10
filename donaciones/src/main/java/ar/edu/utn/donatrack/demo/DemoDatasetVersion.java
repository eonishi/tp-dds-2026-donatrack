package ar.edu.utn.donatrack.demo;

import javax.persistence.Entity;
import javax.persistence.Id;

/** Durable marker committed with fixtures, so restarts never reset users' demo interactions. */
@Entity
public class DemoDatasetVersion {
  @Id private String id;

  protected DemoDatasetVersion() {}

  DemoDatasetVersion(String id) {
    this.id = id;
  }
}

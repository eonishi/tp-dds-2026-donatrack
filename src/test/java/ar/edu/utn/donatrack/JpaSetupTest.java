package ar.edu.utn.donatrack;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

public class JpaSetupTest implements WithSimplePersistenceUnit {
  @Test
  public void testContextLoads() {
    entityManager().clear();
  }
}

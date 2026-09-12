package ar.edu.utn.donatrack;

import org.junit.jupiter.api.Test;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

public class JpaSetupTest implements WithSimplePersistenceUnit {
    @Test
    public void testContextLoads() {
        entityManager().clear();
    }
}

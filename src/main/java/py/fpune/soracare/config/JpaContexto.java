package py.fpune.soracare.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class JpaContexto {

    public static final String UNIDAD_PERSISTENCIA = "SoraCarePU";

    private static EntityManagerFactory fabrica;

    private JpaContexto() {
        // Evita crear objetos de esta clase.
    }

    public static synchronized void inicializar() {
        if (fabrica == null || !fabrica.isOpen()) {
            fabrica = Persistence.createEntityManagerFactory(UNIDAD_PERSISTENCIA);
        }
    }

    public static EntityManager crearEntityManager() {
        inicializar();
        return fabrica.createEntityManager();
    }

    public static synchronized void cerrar() {
        if (fabrica != null && fabrica.isOpen()) {
            fabrica.close();
        }
        fabrica = null;
    }
}

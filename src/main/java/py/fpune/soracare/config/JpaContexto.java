package py.fpune.soracare.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.Objects;
import java.util.function.Function;

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

    public static <T> T ejecutarConsulta(
            Function<EntityManager, T> operacion
    ) {
        Objects.requireNonNull(
                operacion,
                "La operación JPA es obligatoria."
        );

        try (EntityManager entityManager = crearEntityManager()) {
            return operacion.apply(entityManager);
        }
    }

    public static <T> T ejecutarEnTransaccion(
            Function<EntityManager, T> operacion
    ) {
        Objects.requireNonNull(
                operacion,
                "La operación JPA es obligatoria."
        );

        return ejecutarConsulta(entityManager -> {
            EntityTransaction transaccion = entityManager.getTransaction();

            try {
                transaccion.begin();
                T resultado = operacion.apply(entityManager);
                transaccion.commit();
                return resultado;
            } catch (RuntimeException error) {
                rollback(transaccion, error);
                throw error;
            }
        });
    }

    public static synchronized void cerrar() {
        if (fabrica != null && fabrica.isOpen()) {
            fabrica.close();
        }
        fabrica = null;
    }

    private static void rollback(
            EntityTransaction transaccion,
            RuntimeException error
    ) {
        if (transaccion != null && transaccion.isActive()) {
            try {
                transaccion.rollback();
            } catch (RuntimeException errorRollback) {
                error.addSuppressed(errorRollback);
            }
        }
    }
}

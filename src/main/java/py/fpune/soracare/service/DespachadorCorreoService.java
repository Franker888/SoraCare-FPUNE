package py.fpune.soracare.service;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Ejecuta envios externos sin bloquear la confirmacion del pago. */
public final class DespachadorCorreoService {

    private static final ExecutorService EJECUTOR =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("soracare-correo-", 0)
                            .factory()
            );

    private DespachadorCorreoService() {
    }

    public static void ejecutar(Runnable tarea) {
        if (tarea == null) {
            throw new IllegalArgumentException("La tarea de correo es obligatoria.");
        }
        EJECUTOR.submit(tarea);
    }

    public static void cerrar() {
        EJECUTOR.shutdown();
        try {
            if (!EJECUTOR.awaitTermination(
                    Duration.ofSeconds(5).toMillis(),
                    TimeUnit.MILLISECONDS
            )) {
                EJECUTOR.shutdownNow();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            EJECUTOR.shutdownNow();
        }
    }
}

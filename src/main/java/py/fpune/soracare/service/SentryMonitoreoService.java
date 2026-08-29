package py.fpune.soracare.service;

import io.sentry.Sentry;
import io.sentry.protocol.SentryId;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SentryMonitoreoService {

    private static final String VARIABLE_DSN = "SORACARE_SENTRY_DSN";
    private static final String VARIABLE_AMBIENTE =
            "SORACARE_SENTRY_ENVIRONMENT";
    private static final String AMBIENTE_PREDETERMINADO = "local";
    private static final String VERSION_APLICACION =
            "soracare-fpune@1.0-SNAPSHOT";
    private static final AtomicBoolean INICIALIZADO =
            new AtomicBoolean(false);

    private SentryMonitoreoService() {
    }

    public static synchronized void inicializar() {
        if (INICIALIZADO.get()) {
            return;
        }

        String dsn = obtenerVariable(VARIABLE_DSN);
        if (dsn.isBlank()) {
            return;
        }

        Sentry.init(opciones -> {
            opciones.setDsn(dsn);
            opciones.setEnvironment(ambienteConfigurado());
            opciones.setRelease(VERSION_APLICACION);
            opciones.setSendDefaultPii(false);
            opciones.setTracesSampleRate(0.0);
            opciones.setMaxBreadcrumbs(0);
            opciones.setAttachStacktrace(true);
            opciones.setDebug(false);
            opciones.addInAppInclude("py.fpune.soracare");
        });
        INICIALIZADO.set(Sentry.isEnabled());
    }

    public static boolean estaConfigurado() {
        return !obtenerVariable(VARIABLE_DSN).isBlank();
    }

    public static boolean estaActivo() {
        return INICIALIZADO.get() && Sentry.isEnabled();
    }

    public static String ambienteConfigurado() {
        String ambiente = obtenerVariable(VARIABLE_AMBIENTE);
        if (ambiente.isBlank()) {
            return AMBIENTE_PREDETERMINADO;
        }
        return ambiente.toLowerCase(Locale.ROOT);
    }

    public static SentryId capturar(
            Throwable error,
            String metodo,
            String ruta
    ) {
        if (error == null || !estaActivo()) {
            return null;
        }

        AtomicReference<SentryId> identificador = new AtomicReference<>();
        Sentry.withScope(alcance -> {
            alcance.setTag("modulo", "aplicacion_web");
            alcance.setTag("metodo_http", normalizarMetodo(metodo));
            alcance.setTag("ruta", normalizarRuta(ruta));
            alcance.setTag(
                    "tipo_error",
                    error.getClass().getSimpleName()
            );
            identificador.set(
                    Sentry.captureException(crearExcepcionSegura(error))
            );
        });
        return identificador.get();
    }

    public static String enviarPruebaAdministrativa() {
        if (!estaActivo()) {
            throw new IllegalStateException(
                    "Sentry todavía no está configurado en esta ejecución."
            );
        }

        SentryId identificador = capturar(
                new IllegalStateException("Prueba controlada de monitoreo"),
                "POST",
                "/admin/integraciones/sentry"
        );
        Sentry.flush(5_000);

        if (identificador == null) {
            throw new IllegalStateException(
                    "Sentry no devolvió un identificador para el evento."
            );
        }
        return identificador.toString();
    }

    public static synchronized void cerrar() {
        if (!INICIALIZADO.getAndSet(false)) {
            return;
        }
        Sentry.close();
    }

    static RuntimeException crearExcepcionSegura(Throwable error) {
        RuntimeException segura = new RuntimeException(
                "Error interno de SoraCare: "
                        + error.getClass().getSimpleName()
        );
        segura.setStackTrace(error.getStackTrace());
        return segura;
    }

    static String normalizarRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return "/desconocida";
        }
        String limpia = ruta.split("\\?", 2)[0]
                .replaceAll("/[0-9]+(?=/|$)", "/{id}")
                .replaceAll(
                        "/[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}(?=/|$)",
                        "/{id}"
                );
        return limpia.length() <= 160
                ? limpia
                : limpia.substring(0, 160);
    }

    private static String normalizarMetodo(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return "DESCONOCIDO";
        }
        return metodo.toUpperCase(Locale.ROOT);
    }

    private static String obtenerVariable(String nombre) {
        String valor = System.getenv(nombre);
        return valor == null ? "" : valor.trim();
    }
}

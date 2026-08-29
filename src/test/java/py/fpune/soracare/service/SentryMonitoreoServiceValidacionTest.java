package py.fpune.soracare.service;

public final class SentryMonitoreoServiceValidacionTest {

    private SentryMonitoreoServiceValidacionTest() {
    }

    public static void main(String[] args) {
        validarAnonimizacionDeErrores();
        validarNormalizacionDeRutas();
        System.out.println("Validaciones de SentryMonitoreoService: OK");
    }

    private static void validarAnonimizacionDeErrores() {
        IllegalStateException original = new IllegalStateException(
                "Paciente 6770838 - adrian@example.com"
        );
        RuntimeException segura =
                SentryMonitoreoService.crearExcepcionSegura(original);

        exigir(
                !segura.getMessage().contains("6770838")
                        && !segura.getMessage().contains("adrian@example.com"),
                "El mensaje enviado a Sentry no debe incluir datos originales."
        );
        exigir(
                segura.getCause() == null,
                "La excepción original no debe adjuntarse como causa."
        );
    }

    private static void validarNormalizacionDeRutas() {
        String normalizada = SentryMonitoreoService.normalizarRuta(
                "/pacientes/12345/documentos/987?token=secreto"
        );
        exigir(
                "/pacientes/{id}/documentos/{id}".equals(normalizada),
                "La ruta debe ocultar identificadores y eliminar consultas."
        );
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}

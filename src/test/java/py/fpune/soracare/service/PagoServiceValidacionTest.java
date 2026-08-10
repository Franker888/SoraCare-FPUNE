package py.fpune.soracare.service;

public class PagoServiceValidacionTest {

    public static void main(String[] args) throws Exception {
        PagoService servicio = new PagoService();

        esperarValidacion(
                () -> servicio.confirmarDigital(0, 1, 1, "DEMO"),
                "Debe rechazar un pago sin identificador valido."
        );
        esperarValidacion(
                () -> servicio.obtenerComprobante(0, 1L, false),
                "Debe rechazar un comprobante sin identificador valido."
        );
        esperarValidacion(
                () -> new ReservaPagoService().reservar(null, null, 0),
                "Debe rechazar una reserva sin usuario valido."
        );

        System.out.println("OK: validaciones iniciales de pagos verificadas.");
    }

    private static void esperarValidacion(Operacion operacion, String mensaje) throws Exception {
        try {
            operacion.ejecutar();
            throw new AssertionError(mensaje);
        } catch (IllegalArgumentException esperado) {
            // Resultado esperado.
        }
    }

    @FunctionalInterface
    private interface Operacion {
        void ejecutar() throws Exception;
    }
}

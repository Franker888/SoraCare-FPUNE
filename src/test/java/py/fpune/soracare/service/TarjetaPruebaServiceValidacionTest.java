package py.fpune.soracare.service;

public class TarjetaPruebaServiceValidacionTest {

    public static void main(String[] args) {
        TarjetaPruebaService servicio = new TarjetaPruebaService();

        TarjetaPruebaService.Resultado visa = servicio.validar(
                "4242 4242 4242 4242", "12/30", "Paciente de Prueba", "123");
        verificar("VISA".equals(visa.marca()) && "4242".equals(visa.ultimos4()),
                "Debe reconocer la tarjeta Visa de prueba.");

        TarjetaPruebaService.Resultado mastercard = servicio.validar(
                "5555-5555-5555-4444", "12/30", "Paciente de Prueba", "456");
        verificar("MASTERCARD".equals(mastercard.marca()) && "4444".equals(mastercard.ultimos4()),
                "Debe reconocer la tarjeta Mastercard de prueba.");

        esperarRechazo(() -> servicio.validar(
                "4111111111111111", "12/30", "Paciente de Prueba", "123"));
        esperarRechazo(() -> servicio.validar(
                "4242424242424242", "01/20", "Paciente de Prueba", "123"));
        esperarRechazo(() -> servicio.validar(
                "4242424242424242", "12/30", "Paciente de Prueba", "12"));

        System.out.println("OK: tarjetas ficticias, vencimiento y CVV/CVC verificados.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void esperarRechazo(Operacion operacion) {
        try {
            operacion.ejecutar();
            throw new AssertionError("La tarjeta de prueba invalida debia ser rechazada.");
        } catch (IllegalArgumentException esperado) {
            // Resultado esperado.
        }
    }

    @FunctionalInterface
    private interface Operacion {
        void ejecutar();
    }
}

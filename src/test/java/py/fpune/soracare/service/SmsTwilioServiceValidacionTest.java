package py.fpune.soracare.service;

public final class SmsTwilioServiceValidacionTest {

    private SmsTwilioServiceValidacionTest() {
    }

    public static void main(String[] args) {
        String formulario = SmsTwilioService.construirFormulario(
                "+595981234567",
                "+15005550006",
                "Recordatorio de cita: 24/08 a las 09:30"
        );

        verificar(formulario.contains("To=%2B595981234567"),
                "El destinatario no fue codificado correctamente.");
        verificar(formulario.contains("From=%2B15005550006"),
                "El remitente no fue codificado correctamente.");
        verificar(formulario.contains("Recordatorio+de+cita"),
                "El mensaje no fue codificado correctamente.");
        verificar("+595981234567".equals(
                        SmsTwilioService.validarTelefono("+595 981234567")),
                "El teléfono válido no fue normalizado.");

        boolean invalido = false;
        try {
            SmsTwilioService.validarTelefono("0981234567");
        } catch (IllegalArgumentException esperado) {
            invalido = true;
        }
        verificar(invalido, "Se aceptó un teléfono sin formato internacional.");

        System.out.println("Validaciones de SmsTwilioService: OK");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}

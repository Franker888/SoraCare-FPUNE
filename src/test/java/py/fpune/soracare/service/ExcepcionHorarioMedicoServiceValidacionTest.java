package py.fpune.soracare.service;

import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;

public final class ExcepcionHorarioMedicoServiceValidacionTest {

    private ExcepcionHorarioMedicoServiceValidacionTest() {
    }

    public static void main(String[] args) throws Exception {
        ExcepcionHorarioMedicoService service = new ExcepcionHorarioMedicoService();

        validarError(service, new RegistroExcepcionHorarioDTO(
                "", "", "2026-08-17", "", "",
                "BLOQUEO_TOTAL", "Vacaciones"),
                "Seleccioná un médico.");

        validarError(service, new RegistroExcepcionHorarioDTO(
                "1", "", "fecha", "", "",
                "BLOQUEO_TOTAL", "Vacaciones"),
                "La fecha de la excepción no es válida.");

        validarError(service, new RegistroExcepcionHorarioDTO(
                "1", "", "2026-08-17", "", "",
                "TIPO_INVALIDO", "Vacaciones"),
                "El tipo de excepción no es válido.");

        System.out.println("Validaciones básicas de excepciones aprobadas.");
    }

    private static void validarError(
            ExcepcionHorarioMedicoService service,
            RegistroExcepcionHorarioDTO datos,
            String esperado
    ) throws Exception {
        try {
            service.registrar(datos);
            throw new AssertionError("Se esperaba: " + esperado);
        } catch (IllegalArgumentException error) {
            if (!esperado.equals(error.getMessage())) {
                throw new AssertionError("Esperado: " + esperado
                        + " | recibido: " + error.getMessage(), error);
            }
        }
    }
}

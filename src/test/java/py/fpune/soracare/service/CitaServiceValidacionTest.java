package py.fpune.soracare.service;

import py.fpune.soracare.dto.SolicitudCitaDTO;

public final class CitaServiceValidacionTest {

    private CitaServiceValidacionTest() {
    }

    public static void main(String[] args) throws Exception {
        CitaService service = new CitaService();

        validarError(service, new SolicitudCitaDTO(
                "", "1", "", "1", "1", "1",
                "2026-08-17", "08:00", "Control", "", "RECEPCION"),
                "Seleccioná un paciente.");

        validarError(service, new SolicitudCitaDTO(
                "1", "1", "", "1", "1", "1",
                "fecha", "08:00", "Control", "", "RECEPCION"),
                "La fecha del turno no es válida.");

        validarError(service, new SolicitudCitaDTO(
                "1", "1", "", "1", "1", "1",
                "2026-08-17", "hora", "Control", "", "RECEPCION"),
                "La hora del turno no es válida.");

        validarError(service, new SolicitudCitaDTO(
                "1", "1", "", "1", "1", "1",
                "2026-08-17", "08:00", "", "", "RECEPCION"),
                "El motivo de consulta es obligatorio.");

        System.out.println("Validaciones básicas de citas aprobadas.");
    }

    private static void validarError(
            CitaService service,
            SolicitudCitaDTO datos,
            String esperado
    ) throws Exception {
        try {
            service.solicitar(datos, 1L);
            throw new AssertionError("Se esperaba: " + esperado);
        } catch (IllegalArgumentException error) {
            if (!esperado.equals(error.getMessage())) {
                throw new AssertionError("Esperado: " + esperado
                        + " | recibido: " + error.getMessage(), error);
            }
        }
    }
}

package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.RegistroHorarioMedicoDTO;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HorarioMedicoServiceValidacionTest {

    private final HorarioMedicoService servicio = new HorarioMedicoService();

    @Test
    void rechazaDatosNulos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrar(null)
        );
    }

    @Test
    void rechazaDiaFueraDeRango() {
        verificarRechazo(
                crearDatosBase("8", "08:00", "12:00", "30", hoy(), null)
        );
    }

    @Test
    void rechazaRangoHorarioInvertido() {
        verificarRechazo(
                crearDatosBase("1", "12:00", "08:00", "30", hoy(), null)
        );
    }

    @Test
    void rechazaDuracionInferiorAlMinimo() {
        verificarRechazo(
                crearDatosBase("1", "08:00", "12:00", "5", hoy(), null)
        );
    }

    @Test
    void rechazaDuracionSuperiorAlRangoDisponible() {
        verificarRechazo(
                crearDatosBase("1", "08:00", "09:00", "90", hoy(), null)
        );
    }

    @Test
    void rechazaFechaInicialInvalida() {
        verificarRechazo(
                crearDatosBase(
                        "1",
                        "08:00",
                        "12:00",
                        "30",
                        "fecha-invalida",
                        null
                )
        );
    }

    @Test
    void rechazaVigenciaInvertida() {
        verificarRechazo(
                crearDatosBase(
                        "1",
                        "08:00",
                        "12:00",
                        "30",
                        hoy(),
                        LocalDate.now().minusDays(1).toString()
                )
        );
    }

    private void verificarRechazo(RegistroHorarioMedicoDTO datos) {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrar(datos)
        );
    }

    private static RegistroHorarioMedicoDTO crearDatosBase(
            String diaSemana,
            String horaInicio,
            String horaFin,
            String duracionTurno,
            String fechaDesde,
            String fechaHasta
    ) {
        return new RegistroHorarioMedicoDTO(
                "1",
                "1",
                "1",
                diaSemana,
                horaInicio,
                horaFin,
                duracionTurno,
                fechaDesde,
                fechaHasta
        );
    }

    private static String hoy() {
        return LocalDate.now().toString();
    }
}

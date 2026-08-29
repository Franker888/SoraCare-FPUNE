package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionDisponibilidadMedicoServiceValidacionTest {

    @Test
    void aceptaUnBloqueoTotalSinHoras() {
        LocalDate fecha = LocalDate.now(DisponibilidadService.ZONA_HORARIA)
                .plusDays(1);

        GestionDisponibilidadMedicoService.BloqueoValidado bloqueo =
                GestionDisponibilidadMedicoService.validarBloqueo(
                        new RegistroExcepcionHorarioDTO(
                                "",
                                "",
                                fecha.toString(),
                                "",
                                "",
                                "BLOQUEO_TOTAL",
                                "Capacitación"
                        )
                );

        assertEquals(fecha, bloqueo.fecha());
        assertEquals(TipoExcepcionHorario.BLOQUEO_TOTAL, bloqueo.tipo());
        assertNull(bloqueo.horaInicio());
        assertNull(bloqueo.horaFin());
    }

    @Test
    void exigeAmbasHorasEnUnBloqueoParcial() {
        LocalDate fecha = LocalDate.now(DisponibilidadService.ZONA_HORARIA)
                .plusDays(1);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> GestionDisponibilidadMedicoService.validarBloqueo(
                        new RegistroExcepcionHorarioDTO(
                                "",
                                "",
                                fecha.toString(),
                                "08:00",
                                "",
                                "BLOQUEO_PARCIAL",
                                "Reunión"
                        )
                )
        );

        assertEquals("Indicá un intervalo de horas válido.", error.getMessage());
    }

    @Test
    void rechazaDisponibilidadExtraDesdeElPortalMedico() {
        LocalDate fecha = LocalDate.now(DisponibilidadService.ZONA_HORARIA)
                .plusDays(1);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> GestionDisponibilidadMedicoService.validarBloqueo(
                        new RegistroExcepcionHorarioDTO(
                                "",
                                "",
                                fecha.toString(),
                                "08:00",
                                "09:00",
                                "DISPONIBILIDAD_EXTRA",
                                "Turno adicional"
                        )
                )
        );

        assertEquals(
                "Desde el portal médico sólo se permiten bloqueos de agenda.",
                error.getMessage()
        );
    }
}

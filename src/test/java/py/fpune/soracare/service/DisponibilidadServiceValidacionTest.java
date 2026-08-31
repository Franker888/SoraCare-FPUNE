package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.SlotDisponibleDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisponibilidadServiceValidacionTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 6, 10);

    @Test
    void divideUnaJornadaEnTurnosCompletos() {
        List<DisponibilidadService.IntervaloTurno> jornada =
                DisponibilidadService.dividirIntervalo(
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        30
                );

        assertEquals(2, jornada.size());
        assertEquals(LocalTime.of(9, 0), jornada.get(1).fin());
    }

    @Test
    void descartaElRestoQueNoCompletaUnTurno() {
        List<DisponibilidadService.IntervaloTurno> cierreDia =
                DisponibilidadService.dividirIntervalo(
                        LocalTime.of(23, 0),
                        LocalTime.of(23, 59),
                        30
                );

        assertEquals(1, cierreDia.size());
        assertEquals(LocalTime.of(23, 30), cierreDia.getFirst().fin());
        assertTrue(DisponibilidadService.dividirIntervalo(
                LocalTime.of(23, 30),
                LocalTime.of(23, 59),
                30
        ).isEmpty());
    }

    @Test
    void unBloqueoTotalActivoOcultaLosTurnosDeSuFecha() {
        ExcepcionHorarioMedico bloqueo = bloqueo(
                FECHA,
                TipoExcepcionHorario.BLOQUEO_TOTAL,
                null,
                null,
                true
        );

        assertTrue(DisponibilidadService.estaBloqueado(
                slot(),
                List.of(bloqueo)
        ));
    }

    @Test
    void unBloqueoDeOtraFechaNoAfectaLaAgenda() {
        ExcepcionHorarioMedico bloqueo = bloqueo(
                FECHA.minusDays(1),
                TipoExcepcionHorario.BLOQUEO_TOTAL,
                null,
                null,
                true
        );

        assertFalse(DisponibilidadService.estaBloqueado(
                slot(),
                List.of(bloqueo)
        ));
    }

    @Test
    void unBloqueoDesactivadoNoAfectaLaAgenda() {
        ExcepcionHorarioMedico bloqueo = bloqueo(
                FECHA,
                TipoExcepcionHorario.BLOQUEO_TOTAL,
                null,
                null,
                false
        );

        assertFalse(DisponibilidadService.estaBloqueado(
                slot(),
                List.of(bloqueo)
        ));
    }

    @Test
    void elBloqueoParcialSoloOcultaTurnosSuperpuestos() {
        ExcepcionHorarioMedico superpuesto = bloqueo(
                FECHA,
                TipoExcepcionHorario.BLOQUEO_PARCIAL,
                LocalTime.of(8, 15),
                LocalTime.of(8, 45),
                true
        );
        ExcepcionHorarioMedico posterior = bloqueo(
                FECHA,
                TipoExcepcionHorario.BLOQUEO_PARCIAL,
                LocalTime.of(8, 30),
                LocalTime.of(9, 0),
                true
        );

        assertTrue(DisponibilidadService.estaBloqueado(
                slot(),
                List.of(superpuesto)
        ));
        assertFalse(DisponibilidadService.estaBloqueado(
                slot(),
                List.of(posterior)
        ));
    }

    @Test
    void unaCitaCanceladaLiberaElTurno() {
        Cita activa = cita("CONFIRMADA", 1L, 100L);
        Cita cancelada = cita("CANCELADA", 1L, 100L);

        assertTrue(DisponibilidadService.estaOcupado(
                slot(),
                List.of(activa)
        ));
        assertFalse(DisponibilidadService.estaOcupado(
                slot(),
                List.of(cancelada)
        ));
    }

    private static SlotDisponibleDTO slot() {
        return new SlotDisponibleDTO(
                1L,
                null,
                1L,
                10,
                100L,
                FECHA,
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                false
        );
    }

    private static ExcepcionHorarioMedico bloqueo(
            LocalDate fecha,
            TipoExcepcionHorario tipo,
            LocalTime inicio,
            LocalTime fin,
            boolean activo
    ) {
        ExcepcionHorarioMedico excepcion = new ExcepcionHorarioMedico();
        excepcion.setIdMedico(1L);
        excepcion.setFecha(fecha);
        excepcion.setTipo(tipo);
        excepcion.setHoraInicio(inicio);
        excepcion.setHoraFin(fin);
        excepcion.setActivo(activo);
        return excepcion;
    }

    private static Cita cita(
            String estado,
            long idMedico,
            long idConsultorio
    ) {
        Cita cita = new Cita();
        cita.setIdMedico(idMedico);
        cita.setIdConsultorio(idConsultorio);
        cita.setCodigoEstado(estado);
        cita.setFechaHoraInicio(
                FECHA.atTime(8, 0)
                        .atZone(DisponibilidadService.ZONA_HORARIA)
                        .toOffsetDateTime()
        );
        cita.setFechaHoraFin(
                FECHA.atTime(8, 30)
                        .atZone(DisponibilidadService.ZONA_HORARIA)
                        .toOffsetDateTime()
        );
        return cita;
    }
}

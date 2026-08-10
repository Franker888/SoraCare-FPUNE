package py.fpune.soracare.service;

import py.fpune.soracare.dto.RegistroHorarioMedicoDTO;

import java.sql.SQLException;
import java.time.LocalDate;

public class HorarioMedicoServiceValidacionTest {

    public static void main(
            String[] args
    ) throws SQLException {

        HorarioMedicoService servicio =
                new HorarioMedicoService();

        verificarRechazo(
                () -> servicio.registrar(
                        null
                ),
                "Datos nulos"
        );

        verificarRechazo(
                () -> servicio.registrar(
                        crearDatosBase(
                                "8",
                                "08:00",
                                "12:00",
                                "30",
                                LocalDate.now().toString(),
                                null
                        )
                ),
                "Día fuera de rango"
        );

        verificarRechazo(
                () -> servicio.registrar(
                        crearDatosBase(
                                "1",
                                "12:00",
                                "08:00",
                                "30",
                                LocalDate.now().toString(),
                                null
                        )
                ),
                "Rango horario invertido"
        );

        verificarRechazo(
                () -> servicio.registrar(
                        crearDatosBase(
                                "1",
                                "08:00",
                                "12:00",
                                "5",
                                LocalDate.now().toString(),
                                null
                        )
                ),
                "Duración inferior al mínimo"
        );

        verificarRechazo(
                () -> servicio.registrar(
                        crearDatosBase(
                                "1",
                                "08:00",
                                "09:00",
                                "90",
                                LocalDate.now().toString(),
                                null
                        )
                ),
                "Duración superior al rango disponible"
        );

        verificarRechazo(
                () -> servicio.registrar(
                        crearDatosBase(
                                "1",
                                "08:00",
                                "12:00",
                                "30",
                                "fecha-invalida",
                                null
                        )
                ),
                "Fecha inicial inválida"
        );

        verificarRechazo(
                () -> servicio.registrar(
                        crearDatosBase(
                                "1",
                                "08:00",
                                "12:00",
                                "30",
                                LocalDate.now().toString(),
                                LocalDate.now()
                                        .minusDays(1)
                                        .toString()
                        )
                ),
                "Vigencia invertida"
        );

        System.out.println(
                "Validaciones de horario médico: OK."
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

    private static void verificarRechazo(
            Operacion operacion,
            String descripcion
    ) throws SQLException {

        try {
            operacion.ejecutar();

            throw new AssertionError(
                    descripcion
                            + " debería ser rechazado."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    descripcion
                            + ": RECHAZADO."
            );
        }
    }

    @FunctionalInterface
    private interface Operacion {

        void ejecutar()
                throws SQLException;
    }
}

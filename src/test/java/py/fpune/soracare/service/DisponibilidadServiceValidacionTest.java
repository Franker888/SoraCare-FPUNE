package py.fpune.soracare.service;

import java.time.LocalTime;
import java.util.List;

public final class DisponibilidadServiceValidacionTest {

    private DisponibilidadServiceValidacionTest() {
    }

    public static void main(String[] args) {
        List<DisponibilidadService.IntervaloTurno> jornada =
                DisponibilidadService.dividirIntervalo(
                        LocalTime.of(8, 0), LocalTime.of(9, 0), 30);
        verificar(jornada.size() == 2, "La jornada debe generar dos turnos.");
        verificar(jornada.get(1).fin().equals(LocalTime.of(9, 0)),
                "El último turno debe finalizar a las 09:00.");

        List<DisponibilidadService.IntervaloTurno> cierreDia =
                DisponibilidadService.dividirIntervalo(
                        LocalTime.of(23, 0), LocalTime.of(23, 59), 30);
        verificar(cierreDia.size() == 1,
                "El intervalo 23:00-23:59 debe generar un solo turno completo.");
        verificar(cierreDia.getFirst().fin().equals(LocalTime.of(23, 30)),
                "El turno nocturno no debe cruzar la medianoche.");

        verificar(DisponibilidadService.dividirIntervalo(
                        LocalTime.of(23, 30), LocalTime.of(23, 59), 30).isEmpty(),
                "Un intervalo incompleto no debe generar turnos.");

        System.out.println("OK: disponibilidad nocturna sin desbordamiento de medianoche.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}

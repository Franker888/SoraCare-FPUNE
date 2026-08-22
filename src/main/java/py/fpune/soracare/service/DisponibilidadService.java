package py.fpune.soracare.service;

import py.fpune.soracare.dao.CitaDAO;
import py.fpune.soracare.dao.ExcepcionHorarioMedicoDAO;
import py.fpune.soracare.dao.HorarioMedicoDAO;
import py.fpune.soracare.dto.DisponibilidadDiaDTO;
import py.fpune.soracare.dto.SlotDisponibleDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.HorarioMedico;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DisponibilidadService {

    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Asuncion");

    private final HorarioMedicoDAO horarioDAO = new HorarioMedicoDAO();
    private final ExcepcionHorarioMedicoDAO excepcionDAO = new ExcepcionHorarioMedicoDAO();
    private final CitaDAO citaDAO = new CitaDAO();
    private final EspecialidadService especialidadService = new EspecialidadService();

    public List<SlotDisponibleDTO> listar(
            LocalDate fecha,
            Long idMedico,
            Integer idEspecialidad
    ) throws SQLException {
        if (fecha == null) throw new IllegalArgumentException("La fecha de disponibilidad es obligatoria.");
        if (fecha.isBefore(LocalDate.now(ZONA_HORARIA))) return List.of();

        List<HorarioMedico> horarios = horarioDAO.listarVigentesEnFecha(fecha);
        List<ExcepcionHorarioMedico> excepciones = excepcionDAO.listarActivasEnFecha(fecha);
        OffsetDateTime desde = fecha.atStartOfDay(ZONA_HORARIA).toOffsetDateTime();
        OffsetDateTime hasta = fecha.plusDays(1).atStartOfDay(ZONA_HORARIA).toOffsetDateTime();
        List<Cita> citas = citaDAO.listarEntre(desde, hasta);

        return listar(fecha, idMedico, idEspecialidad, horarios, excepciones, citas);
    }

    public List<LocalDate> listarFechasDisponibles(
            LocalDate desde,
            int cantidadDias,
            int limite,
            Long idMedico,
            Integer idEspecialidad
    ) throws SQLException {
        return listarAgendaDisponible(
                desde, cantidadDias, limite, idMedico, idEspecialidad
        ).stream().map(DisponibilidadDiaDTO::fecha).toList();
    }

    public List<DisponibilidadDiaDTO> listarAgendaDisponible(
            LocalDate desde,
            int cantidadDias,
            int limiteDias,
            Long idMedico,
            Integer idEspecialidad
    ) throws SQLException {
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        LocalDate inicio = desde == null || desde.isBefore(hoy) ? hoy : desde;
        if (cantidadDias < 1 || cantidadDias > 365
                || limiteDias < 1 || limiteDias > 100) {
            throw new IllegalArgumentException(
                    "El rango de disponibilidad solicitado no es válido."
            );
        }

        LocalDate finExclusivo = inicio.plusDays(cantidadDias);
        List<HorarioMedico> horarios = horarioDAO.listarTodos();
        List<ExcepcionHorarioMedico> excepciones = excepcionDAO.listarTodas();
        List<Cita> citas = citaDAO.listarEntre(
                inicio.atStartOfDay(ZONA_HORARIA).toOffsetDateTime(),
                finExclusivo.atStartOfDay(ZONA_HORARIA).toOffsetDateTime()
        );
        List<DisponibilidadDiaDTO> agenda = new ArrayList<>();

        for (LocalDate fecha = inicio;
             fecha.isBefore(finExclusivo) && agenda.size() < limiteDias;
             fecha = fecha.plusDays(1)) {
            List<SlotDisponibleDTO> turnos = listar(
                    fecha, idMedico, idEspecialidad,
                    horarios, excepciones, citas
            );
            if (!turnos.isEmpty()) {
                agenda.add(new DisponibilidadDiaDTO(fecha, turnos));
            }
        }
        return List.copyOf(agenda);
    }

    private List<SlotDisponibleDTO> listar(
            LocalDate fecha,
            Long idMedico,
            Integer idEspecialidad,
            List<HorarioMedico> horarios,
            List<ExcepcionHorarioMedico> excepciones,
            List<Cita> citas
    ) throws SQLException {
        Map<String, SlotDisponibleDTO> slots = new LinkedHashMap<>();

        for (HorarioMedico horario : horarios) {
            if (!estaVigente(horario, fecha)) continue;
            if (horario.getDiaSemana() != fecha.getDayOfWeek().getValue()) continue;
            if (!coincideFiltro(horario.getIdMedico(), horario.getIdEspecialidad(), idMedico, idEspecialidad)) continue;
            generarSlots(slots, horario.getIdHorario(), null, horario.getIdMedico(),
                    horario.getIdEspecialidad(), horario.getIdConsultorio(), fecha,
                    horario.getHoraInicio(), horario.getHoraFin(),
                    horario.getDuracionTurnoMinutos(), false, excepciones, citas);
        }

        for (ExcepcionHorarioMedico excepcion : excepciones) {
            if (!excepcion.isActivo() || !fecha.equals(excepcion.getFecha())) continue;
            if (excepcion.getTipo() != TipoExcepcionHorario.DISPONIBILIDAD_EXTRA) continue;
            if (!coincideFiltro(excepcion.getIdMedico(), excepcion.getIdEspecialidad(), idMedico, idEspecialidad)) continue;
            Especialidad especialidad = especialidadService.buscarPorId(excepcion.getIdEspecialidad())
                    .orElse(null);
            if (especialidad == null || !especialidad.isActivo()) continue;
            generarSlots(slots, null, excepcion.getIdExcepcion(), excepcion.getIdMedico(),
                    excepcion.getIdEspecialidad(), excepcion.getIdConsultorio(), fecha,
                    excepcion.getHoraInicio(), excepcion.getHoraFin(),
                    especialidad.getDuracionConsultaMinutos(), true, excepciones, citas);
        }

        return slots.values().stream()
                .sorted(Comparator.comparing(SlotDisponibleDTO::horaInicio)
                        .thenComparing(SlotDisponibleDTO::idMedico)
                        .thenComparing(SlotDisponibleDTO::idConsultorio))
                .toList();
    }

    private static boolean estaVigente(HorarioMedico horario, LocalDate fecha) {
        return horario.isActivo()
                && !horario.getFechaDesde().isAfter(fecha)
                && (horario.getFechaHasta() == null || !horario.getFechaHasta().isBefore(fecha));
    }

    private void generarSlots(
            Map<String, SlotDisponibleDTO> destino,
            Long idHorario,
            Long idExcepcion,
            long idMedico,
            int idEspecialidad,
            long idConsultorio,
            LocalDate fecha,
            LocalTime inicio,
            LocalTime fin,
            int duracion,
            boolean extra,
            List<ExcepcionHorarioMedico> excepciones,
            List<Cita> citas
    ) {
        if (inicio == null || fin == null || duracion <= 0 || !inicio.isBefore(fin)) return;
        OffsetDateTime ahora = OffsetDateTime.now(ZONA_HORARIA);
        for (IntervaloTurno intervalo : dividirIntervalo(inicio, fin, duracion)) {
            SlotDisponibleDTO slot = new SlotDisponibleDTO(idHorario, idExcepcion,
                    idMedico, idEspecialidad, idConsultorio, fecha,
                    intervalo.inicio(), intervalo.fin(), extra);
            OffsetDateTime inicioReal = fecha.atTime(intervalo.inicio())
                    .atZone(ZONA_HORARIA).toOffsetDateTime();
            if (inicioReal.isAfter(ahora)
                    && !estaBloqueado(slot, excepciones)
                    && !estaOcupado(slot, citas)) {
                destino.putIfAbsent(clave(slot), slot);
            }
        }
    }

    static List<IntervaloTurno> dividirIntervalo(
            LocalTime inicio, LocalTime fin, int duracionMinutos
    ) {
        if (inicio == null || fin == null || duracionMinutos <= 0
                || !inicio.isBefore(fin)) {
            return List.of();
        }
        LocalDate fechaReferencia = LocalDate.of(2000, 1, 1);
        LocalDateTime cursor = fechaReferencia.atTime(inicio);
        LocalDateTime limite = fechaReferencia.atTime(fin);
        List<IntervaloTurno> intervalos = new ArrayList<>();
        while (!cursor.plusMinutes(duracionMinutos).isAfter(limite)) {
            LocalDateTime finTurno = cursor.plusMinutes(duracionMinutos);
            intervalos.add(new IntervaloTurno(cursor.toLocalTime(), finTurno.toLocalTime()));
            cursor = finTurno;
        }
        return List.copyOf(intervalos);
    }

    private static boolean estaBloqueado(
            SlotDisponibleDTO slot,
            List<ExcepcionHorarioMedico> excepciones
    ) {
        for (ExcepcionHorarioMedico excepcion : excepciones) {
            if (excepcion.getTipo() == TipoExcepcionHorario.DISPONIBILIDAD_EXTRA) continue;
            if (excepcion.getIdMedico() != slot.idMedico()) continue;
            if (excepcion.getIdEspecialidad() != null
                    && excepcion.getIdEspecialidad() != slot.idEspecialidad()) continue;
            if (excepcion.getIdConsultorio() != null
                    && excepcion.getIdConsultorio() != slot.idConsultorio()) continue;
            if (excepcion.getTipo() == TipoExcepcionHorario.BLOQUEO_TOTAL) return true;
            if (seSuperpone(slot.horaInicio(), slot.horaFin(),
                    excepcion.getHoraInicio(), excepcion.getHoraFin())) return true;
        }
        return false;
    }

    private static boolean estaOcupado(SlotDisponibleDTO slot, List<Cita> citas) {
        OffsetDateTime inicio = slot.fecha().atTime(slot.horaInicio())
                .atZone(ZONA_HORARIA).toOffsetDateTime();
        OffsetDateTime fin = slot.fecha().atTime(slot.horaFin())
                .atZone(ZONA_HORARIA).toOffsetDateTime();
        for (Cita cita : citas) {
            if ("CANCELADA".equals(cita.getCodigoEstado())) continue;
            if (cita.getIdMedico() != slot.idMedico()
                    && cita.getIdConsultorio() != slot.idConsultorio()) continue;
            if (inicio.isBefore(cita.getFechaHoraFin()) && fin.isAfter(cita.getFechaHoraInicio())) return true;
        }
        return false;
    }

    private static boolean seSuperpone(LocalTime inicioA, LocalTime finA,
                                       LocalTime inicioB, LocalTime finB) {
        return inicioB != null && finB != null
                && inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    private static boolean coincideFiltro(long medico, int especialidad,
                                           Long filtroMedico, Integer filtroEspecialidad) {
        return (filtroMedico == null || medico == filtroMedico)
                && (filtroEspecialidad == null || especialidad == filtroEspecialidad);
    }

    private static String clave(SlotDisponibleDTO slot) {
        return slot.idMedico() + ":" + slot.idEspecialidad() + ":"
                + slot.idConsultorio() + ":" + slot.fecha() + ":" + slot.horaInicio();
    }

    record IntervaloTurno(LocalTime inicio, LocalTime fin) {
    }
}

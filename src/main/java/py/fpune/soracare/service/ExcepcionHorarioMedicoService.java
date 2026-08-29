package py.fpune.soracare.service;

import py.fpune.soracare.dao.ExcepcionHorarioMedicoDAO;
import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class ExcepcionHorarioMedicoService {

    private final ExcepcionHorarioMedicoDAO excepcionDAO =
            new ExcepcionHorarioMedicoDAO();
    private final MedicoService medicoService = new MedicoService();
    private final MedicoEspecialidadService relacionService =
            new MedicoEspecialidadService();
    private final ConsultorioService consultorioService = new ConsultorioService();

    public ExcepcionHorarioMedico registrar(RegistroExcepcionHorarioDTO datos)
            throws SQLException {
        if (datos == null) throw new IllegalArgumentException("Los datos de la excepción son obligatorios.");

        String[] relacion = separarRelacion(datos.medicoEspecialidad());
        long idMedico = convertirLong(relacion[0], "Seleccioná un médico.");
        Integer idEspecialidad = convertirEnteroOpcional(relacion[1]);
        Long idConsultorio = convertirLongOpcional(datos.idConsultorio());
        LocalDate fecha = convertirFecha(datos.fecha());
        TipoExcepcionHorario tipo = convertirTipo(datos.tipo());
        LocalTime inicio = convertirHoraOpcional(datos.horaInicio());
        LocalTime fin = convertirHoraOpcional(datos.horaFin());
        String motivo = normalizarMotivo(datos.motivo());

        if (medicoService.buscarPorId(idMedico).isEmpty()) {
            throw new IllegalArgumentException("El médico seleccionado no existe.");
        }

        if (idEspecialidad != null) {
            MedicoEspecialidad asignacion = relacionService.buscarPorIds(idMedico, idEspecialidad)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La especialidad no está asignada al médico."));
            if (!asignacion.isActivo()) {
                throw new IllegalArgumentException("La asignación médico-especialidad está inactiva.");
            }
        }

        if (idConsultorio != null && consultorioService.buscarPorId(idConsultorio).isEmpty()) {
            throw new IllegalArgumentException("El consultorio seleccionado no existe.");
        }

        validarDatosPorTipo(tipo, idEspecialidad, idConsultorio, inicio, fin);

        ExcepcionHorarioMedico excepcion = new ExcepcionHorarioMedico();
        excepcion.setIdMedico(idMedico);
        excepcion.setIdEspecialidad(idEspecialidad);
        excepcion.setIdConsultorio(idConsultorio);
        excepcion.setFecha(fecha);
        excepcion.setHoraInicio(inicio);
        excepcion.setHoraFin(fin);
        excepcion.setTipo(tipo);
        excepcion.setMotivo(motivo);

        return excepcionDAO.crear(excepcion);
    }

    public List<ExcepcionHorarioMedico> listarTodas() throws SQLException {
        return excepcionDAO.listarTodas();
    }

    public List<ExcepcionHorarioMedico> listarActivasEnFecha(LocalDate fecha)
            throws SQLException {
        return excepcionDAO.listarActivasEnFecha(fecha);
    }

    public List<ExcepcionHorarioMedico> listarPorMedico(long idMedico)
            throws SQLException {
        if (idMedico <= 0) {
            throw new IllegalArgumentException("El médico seleccionado no es válido.");
        }
        return excepcionDAO.listarPorMedico(idMedico);
    }

    public Optional<ExcepcionHorarioMedico> buscarPorId(long idExcepcion)
            throws SQLException {
        if (idExcepcion <= 0) {
            throw new IllegalArgumentException("La excepción seleccionada no es válida.");
        }
        return excepcionDAO.buscarPorId(idExcepcion);
    }

    public void actualizarEstado(long idExcepcion, boolean activo) throws SQLException {
        if (idExcepcion <= 0) throw new IllegalArgumentException("La excepción seleccionada no es válida.");
        if (excepcionDAO.buscarPorId(idExcepcion).isEmpty()) {
            throw new IllegalArgumentException("La excepción seleccionada no existe.");
        }
        excepcionDAO.actualizarActivo(idExcepcion, activo);
    }

    private static void validarDatosPorTipo(
            TipoExcepcionHorario tipo,
            Integer idEspecialidad,
            Long idConsultorio,
            LocalTime inicio,
            LocalTime fin
    ) {
        if (tipo == TipoExcepcionHorario.BLOQUEO_TOTAL) {
            if (inicio != null || fin != null) {
                throw new IllegalArgumentException("El bloqueo total no debe indicar horas.");
            }
            return;
        }
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("Indicá un intervalo de horas válido.");
        }
        if (tipo == TipoExcepcionHorario.DISPONIBILIDAD_EXTRA
                && (idEspecialidad == null || idConsultorio == null)) {
            throw new IllegalArgumentException(
                    "La disponibilidad extra requiere especialidad y consultorio.");
        }
    }

    private static String[] separarRelacion(String valor) {
        if (valor == null || valor.isBlank()) return new String[]{null, null};
        String[] partes = valor.strip().split(":", -1);
        if (partes.length == 1) return new String[]{partes[0], null};
        return partes.length == 2 ? partes : new String[]{null, null};
    }

    private static long convertirLong(String valor, String mensaje) {
        try {
            long numero = Long.parseLong(valor == null ? "" : valor.strip());
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensaje, error);
        }
    }

    private static Long convertirLongOpcional(String valor) {
        return valor == null || valor.isBlank() ? null
                : convertirLong(valor, "El consultorio seleccionado no es válido.");
    }

    private static Integer convertirEnteroOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La especialidad seleccionada no es válida.", error);
        }
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return LocalDate.parse(valor == null ? "" : valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha de la excepción no es válida.", error);
        }
    }

    private static LocalTime convertirHoraOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalTime.parse(valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La hora indicada no es válida.", error);
        }
    }

    private static TipoExcepcionHorario convertirTipo(String valor) {
        try {
            return TipoExcepcionHorario.valueOf(valor == null ? "" : valor.strip());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("El tipo de excepción no es válido.", error);
        }
    }

    private static String normalizarMotivo(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El motivo de la excepción es obligatorio.");
        }
        String motivo = valor.strip();
        if (motivo.length() > 255) {
            throw new IllegalArgumentException("El motivo no puede superar 255 caracteres.");
        }
        return motivo;
    }
}

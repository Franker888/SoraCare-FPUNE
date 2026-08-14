package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.AgendaMedicoDAO;
import py.fpune.soracare.dao.AtencionMedicaDAO;
import py.fpune.soracare.dao.CitaDAO;
import py.fpune.soracare.dao.EstadoCitaDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.PacienteDAO;
import py.fpune.soracare.dto.AtencionMedicaDTO;
import py.fpune.soracare.dto.CitaAgendaMedicoDTO;
import py.fpune.soracare.dto.PanelClinicoPacienteDTO;
import py.fpune.soracare.dto.RegistroAtencionMedicaDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.EstadoCita;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.Paciente;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class AtencionMedicaService {

    private static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    private static final String ESTADO_EN_ATENCION = "EN_ATENCION";
    private static final String ESTADO_ATENDIDA = "ATENDIDA";
    private static final int LIMITE_TEXTO_CLINICO = 6000;

    private final MedicoDAO medicoDAO = new MedicoDAO();
    private final PacienteDAO pacienteDAO = new PacienteDAO();
    private final CitaDAO citaDAO = new CitaDAO();
    private final EstadoCitaDAO estadoCitaDAO = new EstadoCitaDAO();
    private final AgendaMedicoDAO agendaMedicoDAO = new AgendaMedicoDAO();
    private final AtencionMedicaDAO atencionMedicaDAO = new AtencionMedicaDAO();

    public PanelClinicoPacienteDTO cargarPanel(
            long idPersonaMedico,
            long idCita
    ) throws SQLException {
        Medico medico = buscarMedicoActivo(idPersonaMedico);
        CitaAgendaMedicoDTO cita = agendaMedicoDAO
                .buscarPorCitaYMedico(idCita, medico.getIdMedico())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cita no existe o no pertenece al médico autenticado."));
        Paciente paciente = pacienteDAO.buscarPorId(cita.idPaciente())
                .orElseThrow(() -> new SQLException(
                        "No fue posible recuperar la ficha del paciente."));
        AtencionMedicaDTO atencionActual = atencionMedicaDAO
                .buscarPorCita(idCita)
                .orElse(null);
        List<AtencionMedicaDTO> historial = atencionMedicaDAO
                .listarHistorialPaciente(paciente.getIdPaciente(), idCita, 30);

        return new PanelClinicoPacienteDTO(
                paciente,
                cita,
                atencionActual,
                historial
        );
    }

    public AtencionMedicaDTO iniciar(
            long idPersonaMedico,
            long idCita,
            long idUsuario
    ) throws SQLException {
        validarIdentificadores(idPersonaMedico, idCita, idUsuario);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                Medico medico = buscarMedicoActivo(conexion, idPersonaMedico);
                Cita cita = buscarCitaPropiaBloqueada(conexion, idCita, medico);

                if (ESTADO_EN_ATENCION.equals(cita.getCodigoEstado())) {
                    AtencionMedicaDTO existente = atencionMedicaDAO
                            .buscarPorCita(conexion, idCita)
                            .orElseThrow(() -> new SQLException(
                                    "La cita figura en atención, pero no posee una ficha clínica."));
                    conexion.commit();
                    return existente;
                }
                if (!ESTADO_CONFIRMADA.equals(cita.getCodigoEstado())) {
                    throw new IllegalArgumentException(
                            "La atención solamente puede iniciarse cuando la cita está confirmada.");
                }

                LocalDate fechaCita = cita.getFechaHoraInicio()
                        .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                        .toLocalDate();
                LocalDate hoy = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
                if (fechaCita.isAfter(hoy)) {
                    throw new IllegalArgumentException(
                            "No se puede iniciar una atención antes de la fecha de la cita.");
                }

                if (atencionMedicaDAO.buscarPorCita(conexion, idCita).isPresent()) {
                    throw new IllegalArgumentException(
                            "La cita ya posee una atención médica registrada.");
                }

                EstadoCita enAtencion = buscarEstado(conexion, ESTADO_EN_ATENCION);
                citaDAO.actualizarEstado(
                        conexion,
                        idCita,
                        enAtencion.getIdEstadoCita(),
                        idUsuario
                );
                AtencionMedicaDTO atencion = atencionMedicaDAO.iniciar(
                        conexion,
                        idCita,
                        cita.getIdPaciente(),
                        medico.getIdMedico(),
                        cita.getMotivoConsulta()
                );
                conexion.commit();
                return atencion;
            } catch (Exception error) {
                rollback(conexion, error);
                propagar(error, "No fue posible iniciar la atención médica.");
                throw new SQLException("No fue posible iniciar la atención médica.");
            }
        }
    }

    public AtencionMedicaDTO guardar(
            long idPersonaMedico,
            long idCita,
            long idUsuario,
            RegistroAtencionMedicaDTO datos
    ) throws SQLException {
        return actualizar(
                idPersonaMedico,
                idCita,
                idUsuario,
                datos,
                false
        );
    }

    public AtencionMedicaDTO finalizar(
            long idPersonaMedico,
            long idCita,
            long idUsuario,
            RegistroAtencionMedicaDTO datos
    ) throws SQLException {
        return actualizar(
                idPersonaMedico,
                idCita,
                idUsuario,
                datos,
                true
        );
    }

    private AtencionMedicaDTO actualizar(
            long idPersonaMedico,
            long idCita,
            long idUsuario,
            RegistroAtencionMedicaDTO datos,
            boolean finalizar
    ) throws SQLException {
        validarIdentificadores(idPersonaMedico, idCita, idUsuario);
        DatosClinicosNormalizados normalizados = normalizar(datos, finalizar);

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                Medico medico = buscarMedicoActivo(conexion, idPersonaMedico);
                Cita cita = buscarCitaPropiaBloqueada(conexion, idCita, medico);
                if (!ESTADO_EN_ATENCION.equals(cita.getCodigoEstado())) {
                    throw new IllegalArgumentException(
                            "La cita no posee una atención médica en curso.");
                }
                AtencionMedicaDTO actual = atencionMedicaDAO
                        .buscarPorCita(conexion, idCita)
                        .orElseThrow(() -> new SQLException(
                                "No existe la ficha clínica de la atención en curso."));
                if (actual.idMedico() != medico.getIdMedico()) {
                    throw new IllegalArgumentException(
                            "La atención no pertenece al médico autenticado.");
                }

                String estadoAtencion = finalizar ? "FINALIZADA" : "EN_CURSO";
                AtencionMedicaDTO actualizada = atencionMedicaDAO.actualizar(
                        conexion,
                        actual.idAtencion(),
                        medico.getIdMedico(),
                        normalizados.sintomas(),
                        normalizados.antecedentesRelevantes(),
                        normalizados.temperaturaC(),
                        normalizados.frecuenciaCardiaca(),
                        normalizados.frecuenciaRespiratoria(),
                        normalizados.presionSistolica(),
                        normalizados.presionDiastolica(),
                        normalizados.saturacionOxigeno(),
                        normalizados.evaluacionAreaAfectada(),
                        normalizados.examenFisico(),
                        normalizados.evaluacionClinica(),
                        normalizados.planTratamiento(),
                        normalizados.indicaciones(),
                        normalizados.observaciones(),
                        normalizados.requiereSeguimiento(),
                        normalizados.fechaProximoControl(),
                        estadoAtencion
                );

                pacienteDAO.actualizarPerfilClinico(
                        conexion,
                        cita.getIdPaciente(),
                        normalizados.antecedentesPatologicosPersonales(),
                        normalizados.farmacosUtilizados(),
                        normalizados.alergias()
                );

                if (finalizar) {
                    EstadoCita atendida = buscarEstado(conexion, ESTADO_ATENDIDA);
                    citaDAO.actualizarEstado(
                            conexion,
                            idCita,
                            atendida.getIdEstadoCita(),
                            idUsuario
                    );
                }

                conexion.commit();
                return actualizada;
            } catch (Exception error) {
                rollback(conexion, error);
                propagar(
                        error,
                        finalizar
                                ? "No fue posible finalizar la atención médica."
                                : "No fue posible guardar la atención médica."
                );
                throw new SQLException("No fue posible actualizar la atención médica.");
            }
        }
    }

    private Medico buscarMedicoActivo(long idPersona) throws SQLException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta médica no es válida.");
        }
        Medico medico = medicoDAO.buscarPorIdPersona(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con un médico registrado."));
        validarMedicoActivo(medico);
        return medico;
    }

    private Medico buscarMedicoActivo(Connection conexion, long idPersona)
            throws SQLException {
        Medico medico = medicoDAO.buscarPorIdPersona(conexion, idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con un médico registrado."));
        validarMedicoActivo(medico);
        return medico;
    }

    private Cita buscarCitaPropiaBloqueada(
            Connection conexion,
            long idCita,
            Medico medico
    ) throws SQLException {
        Cita cita = citaDAO.buscarPorIdParaActualizar(conexion, idCita)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cita seleccionada no existe."));
        if (!Long.valueOf(medico.getIdMedico()).equals(cita.getIdMedico())) {
            throw new IllegalArgumentException(
                    "No podés acceder a la atención asignada a otro médico.");
        }
        return cita;
    }

    private EstadoCita buscarEstado(Connection conexion, String codigo)
            throws SQLException {
        return estadoCitaDAO.buscarPorCodigo(conexion, codigo)
                .orElseThrow(() -> new SQLException(
                        "No existe el estado de cita " + codigo + "."));
    }

    private static DatosClinicosNormalizados normalizar(
            RegistroAtencionMedicaDTO datos,
            boolean finalizar
    ) {
        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la atención son obligatorios.");
        }
        String sintomas = textoOpcional(datos.sintomas(), "Síntomas");
        String antecedentes = textoOpcional(
                datos.antecedentesRelevantes(), "Antecedentes relevantes");
        String antecedentesPatologicos = textoOpcional(
                datos.antecedentesPatologicosPersonales(),
                "Antecedentes patológicos personales");
        String farmacos = textoOpcional(
                datos.farmacosUtilizados(), "Fármacos utilizados");
        String alergias = textoOpcional(datos.alergias(), "Alergias");
        BigDecimal temperatura = decimalOpcional(
                datos.temperaturaC(), "Temperatura", new BigDecimal("30.0"),
                new BigDecimal("45.0"));
        Integer frecuenciaCardiaca = enteroOpcional(
                datos.frecuenciaCardiaca(), "Frecuencia cardíaca", 20, 250);
        Integer frecuenciaRespiratoria = enteroOpcional(
                datos.frecuenciaRespiratoria(), "Frecuencia respiratoria", 5, 80);
        Integer presionSistolica = enteroOpcional(
                datos.presionSistolica(), "Presión sistólica", 50, 300);
        Integer presionDiastolica = enteroOpcional(
                datos.presionDiastolica(), "Presión diastólica", 30, 200);
        Integer saturacionOxigeno = enteroOpcional(
                datos.saturacionOxigeno(), "Saturación de oxígeno", 0, 100);
        String evaluacionAreaAfectada = textoOpcional(
                datos.evaluacionAreaAfectada(), "Evaluación del área afectada");
        String examen = textoOpcional(datos.examenFisico(), "Examen físico");
        String evaluacion = textoOpcional(
                datos.evaluacionClinica(), "Evaluación clínica");
        String plan = textoOpcional(datos.planTratamiento(), "Plan de tratamiento");
        String indicaciones = textoOpcional(datos.indicaciones(), "Indicaciones");
        String observaciones = textoOpcional(datos.observaciones(), "Observaciones");
        boolean seguimiento = convertirBooleano(datos.requiereSeguimiento());
        LocalDate proximoControl = convertirFechaControl(
                datos.fechaProximoControl(), seguimiento);

        if (presionSistolica != null && presionDiastolica != null
                && presionSistolica <= presionDiastolica) {
            throw new IllegalArgumentException(
                    "La presión sistólica debe ser mayor que la presión diastólica.");
        }

        if (finalizar && (temperatura == null
                || frecuenciaCardiaca == null
                || frecuenciaRespiratoria == null
                || presionSistolica == null
                || presionDiastolica == null
                || saturacionOxigeno == null)) {
            throw new IllegalArgumentException(
                    "Completá todos los signos vitales antes de finalizar la atención.");
        }

        if (finalizar && evaluacionAreaAfectada == null) {
            throw new IllegalArgumentException(
                    "Completá la evaluación del área afectada antes de finalizar.");
        }
        if (finalizar && examen == null) {
            throw new IllegalArgumentException(
                    "Completá el examen físico general antes de finalizar.");
        }

        if (finalizar && evaluacion == null) {
            throw new IllegalArgumentException(
                    "Completá la evaluación o diagnóstico clínico antes de finalizar.");
        }
        if (finalizar && plan == null) {
            throw new IllegalArgumentException(
                    "Completá el plan de tratamiento antes de finalizar.");
        }
        if (finalizar && indicaciones == null) {
            throw new IllegalArgumentException(
                    "Completá las indicaciones antes de finalizar.");
        }

        return new DatosClinicosNormalizados(
                sintomas,
                antecedentes,
                antecedentesPatologicos,
                farmacos,
                alergias,
                temperatura,
                frecuenciaCardiaca,
                frecuenciaRespiratoria,
                presionSistolica,
                presionDiastolica,
                saturacionOxigeno,
                evaluacionAreaAfectada,
                examen,
                evaluacion,
                plan,
                indicaciones,
                observaciones,
                seguimiento,
                proximoControl
        );
    }

    private static String textoOpcional(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.strip();
        if (texto.length() > LIMITE_TEXTO_CLINICO) {
            throw new IllegalArgumentException(
                    nombreCampo + " no puede superar "
                            + LIMITE_TEXTO_CLINICO + " caracteres.");
        }
        return texto;
    }

    private static BigDecimal decimalOpcional(
            String valor,
            String nombreCampo,
            BigDecimal minimo,
            BigDecimal maximo
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            BigDecimal numero = new BigDecimal(valor.strip().replace(',', '.'))
                    .stripTrailingZeros();
            if (numero.scale() > 1) {
                throw new IllegalArgumentException(
                        nombreCampo + " admite como máximo un decimal.");
            }
            if (numero.compareTo(minimo) < 0 || numero.compareTo(maximo) > 0) {
                throw new IllegalArgumentException(
                        nombreCampo + " debe estar entre " + minimo + " y " + maximo + ".");
            }
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser un número válido.", error);
        }
    }

    private static Integer enteroOpcional(
            String valor,
            String nombreCampo,
            int minimo,
            int maximo
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException(
                        nombreCampo + " debe estar entre " + minimo + " y " + maximo + ".");
            }
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser un número entero válido.", error);
        }
    }

    private static boolean convertirBooleano(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        return switch (valor.strip().toLowerCase(Locale.ROOT)) {
            case "true", "on", "1", "si", "sí" -> true;
            default -> false;
        };
    }

    private static LocalDate convertirFechaControl(
            String valor,
            boolean seguimiento
    ) {
        if (!seguimiento) {
            return null;
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Indicá la fecha del próximo control.");
        }
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            if (fecha.isBefore(LocalDate.now(DisponibilidadService.ZONA_HORARIA))) {
                throw new IllegalArgumentException(
                        "La fecha del próximo control no puede estar en el pasado.");
            }
            return fecha;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La fecha del próximo control no es válida.", error);
        }
    }

    private static void validarMedicoActivo(Medico medico) {
        if (medico.getEstado() != EstadoMedico.ACTIVO) {
            throw new IllegalArgumentException(
                    "El médico no se encuentra activo para registrar atenciones.");
        }
    }

    private static void validarIdentificadores(
            long idPersona,
            long idCita,
            long idUsuario
    ) {
        if (idPersona <= 0 || idCita <= 0 || idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "La sesión o la cita seleccionada no son válidas.");
        }
    }

    private static void propagar(Exception error, String mensaje)
            throws SQLException {
        if (error instanceof IllegalArgumentException validacion) {
            throw validacion;
        }
        if (error instanceof SQLException sql) {
            throw sql;
        }
        throw new SQLException(mensaje, error);
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    private record DatosClinicosNormalizados(
            String sintomas,
            String antecedentesRelevantes,
            String antecedentesPatologicosPersonales,
            String farmacosUtilizados,
            String alergias,
            BigDecimal temperaturaC,
            Integer frecuenciaCardiaca,
            Integer frecuenciaRespiratoria,
            Integer presionSistolica,
            Integer presionDiastolica,
            Integer saturacionOxigeno,
            String evaluacionAreaAfectada,
            String examenFisico,
            String evaluacionClinica,
            String planTratamiento,
            String indicaciones,
            String observaciones,
            boolean requiereSeguimiento,
            LocalDate fechaProximoControl
    ) {
    }
}

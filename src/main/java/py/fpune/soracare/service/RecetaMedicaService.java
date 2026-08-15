package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.AtencionMedicaDAO;
import py.fpune.soracare.dao.MedicamentoDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.NotificacionDAO;
import py.fpune.soracare.dao.RecetaMedicaDAO;
import py.fpune.soracare.dto.AtencionMedicaDTO;
import py.fpune.soracare.dto.RecetaMedicaDTO;
import py.fpune.soracare.dto.RegistroDetalleRecetaDTO;
import py.fpune.soracare.dto.RegistroRecetaMedicaDTO;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medicamento;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.Paciente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecetaMedicaService {

    private static final int MAXIMO_MEDICAMENTOS = 10;
    private final MedicoDAO medicoDAO = new MedicoDAO();
    private final AtencionMedicaDAO atencionMedicaDAO = new AtencionMedicaDAO();
    private final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    private final RecetaMedicaDAO recetaMedicaDAO = new RecetaMedicaDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final PacienteService pacienteService = new PacienteService();

    public RecetaMedicaDTO emitir(
            long idPersonaMedico,
            long idCita,
            RegistroRecetaMedicaDTO datos
    ) throws SQLException {
        if (idPersonaMedico <= 0 || idCita <= 0) {
            throw new IllegalArgumentException("La sesión médica o la cita no son válidas.");
        }
        DatosNormalizados normalizados = normalizar(datos);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                Medico medico = buscarMedicoActivo(conexion, idPersonaMedico);
                AtencionMedicaDTO atencion = atencionMedicaDAO.buscarPorCita(conexion, idCita)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La cita todavía no posee una atención médica iniciada."));
                if (atencion.idMedico() != medico.getIdMedico()) {
                    throw new IllegalArgumentException(
                            "No podés emitir recetas para una atención de otro médico.");
                }
                if (!"EN_CURSO".equals(atencion.estado())) {
                    throw new IllegalArgumentException(
                            "Las recetas nuevas deben emitirse mientras la atención está en curso.");
                }

                List<RecetaMedicaDAO.DetalleNuevo> detalles = new ArrayList<>();
                for (DetalleNormalizado detalle : normalizados.detalles()) {
                    Medicamento medicamento = medicamentoDAO
                            .buscarPorId(conexion, detalle.idMedicamento())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Uno de los medicamentos seleccionados ya no existe."));
                    if (!medicamento.isActivo()) {
                        throw new IllegalArgumentException(
                                medicamento.getNombreComercial()
                                        + " está inactivo y no puede recetarse.");
                    }
                    detalles.add(new RecetaMedicaDAO.DetalleNuevo(
                            detalle.idMedicamento(), detalle.dosis(), detalle.frecuencia(),
                            detalle.duracionDias(), detalle.cantidad(), detalle.unidad(),
                            detalle.instrucciones()));
                }

                RecetaMedicaDTO receta = recetaMedicaDAO.crear(
                        conexion,
                        atencion.idAtencion(),
                        atencion.idPaciente(),
                        atencion.idMedico(),
                        normalizados.fechaVencimiento(),
                        normalizados.indicacionesGenerales(),
                        detalles
                );
                notificacionDAO.registrarRecetaEmitida(conexion, receta);
                conexion.commit();
                return receta;
            } catch (Exception error) {
                rollback(conexion, error);
                propagar(error, "No fue posible emitir la receta médica.");
                throw new SQLException("No fue posible emitir la receta médica.");
            }
        }
    }

    public void anular(
            long idPersonaMedico,
            long idUsuario,
            long idReceta,
            String motivo
    ) throws SQLException {
        if (idPersonaMedico <= 0 || idUsuario <= 0 || idReceta <= 0) {
            throw new IllegalArgumentException("La receta seleccionada no es válida.");
        }
        String motivoLimpio = textoObligatorio(
                motivo, "Indicá el motivo de anulación.", 500);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                Medico medico = buscarMedicoActivo(conexion, idPersonaMedico);
                recetaMedicaDAO.anular(
                        conexion, idReceta, medico.getIdMedico(), idUsuario, motivoLimpio);
                conexion.commit();
            } catch (Exception error) {
                rollback(conexion, error);
                propagar(error, "No fue posible anular la receta médica.");
            }
        }
    }

    public List<RecetaMedicaDTO> listarPorAtencion(long idAtencion) throws SQLException {
        if (idAtencion <= 0) return List.of();
        return recetaMedicaDAO.listarPorAtencion(idAtencion);
    }

    public List<RecetaMedicaDTO> listarParaPaciente(long idPersonaPaciente)
            throws SQLException {
        Paciente paciente = pacienteService.buscarPorIdPersona(idPersonaPaciente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no posee una ficha de paciente asociada."));
        return recetaMedicaDAO.listarPorPaciente(paciente.getIdPaciente());
    }

    public RecetaMedicaDTO buscarParaPaciente(long idReceta, long idPersonaPaciente)
            throws SQLException {
        Paciente paciente = pacienteService.buscarPorIdPersona(idPersonaPaciente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no posee una ficha de paciente asociada."));
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            RecetaMedicaDTO receta = recetaMedicaDAO.buscarPorId(conexion, idReceta)
                    .orElseThrow(() -> new IllegalArgumentException("La receta no existe."));
            if (receta.idPaciente() != paciente.getIdPaciente()) {
                throw new IllegalArgumentException("No tenés autorización para consultar esta receta.");
            }
            return receta;
        }
    }

    public RecetaMedicaDTO buscarParaMedico(long idReceta, long idPersonaMedico)
            throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            Medico medico = buscarMedicoActivo(conexion, idPersonaMedico);
            RecetaMedicaDTO receta = recetaMedicaDAO.buscarPorId(conexion, idReceta)
                    .orElseThrow(() -> new IllegalArgumentException("La receta no existe."));
            if (receta.idMedico() != medico.getIdMedico()) {
                throw new IllegalArgumentException("No tenés autorización para consultar esta receta.");
            }
            return receta;
        }
    }

    private Medico buscarMedicoActivo(Connection conexion, long idPersona)
            throws SQLException {
        Medico medico = medicoDAO.buscarPorIdPersona(conexion, idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con un médico registrado."));
        if (medico.getEstado() != EstadoMedico.ACTIVO) {
            throw new IllegalArgumentException("El médico no se encuentra activo.");
        }
        return medico;
    }

    private static DatosNormalizados normalizar(RegistroRecetaMedicaDTO datos) {
        if (datos == null || datos.detalles().isEmpty()) {
            throw new IllegalArgumentException("Agregá al menos un medicamento a la receta.");
        }
        if (datos.detalles().size() > MAXIMO_MEDICAMENTOS) {
            throw new IllegalArgumentException(
                    "Una receta no puede contener más de " + MAXIMO_MEDICAMENTOS + " medicamentos.");
        }
        LocalDate vencimiento = fechaOpcional(datos.fechaVencimiento());
        String indicaciones = textoOpcional(datos.indicacionesGenerales(), 4000,
                "Las indicaciones generales");
        List<DetalleNormalizado> detalles = new ArrayList<>();
        Set<Long> medicamentos = new HashSet<>();
        for (RegistroDetalleRecetaDTO detalle : datos.detalles()) {
            long idMedicamento = enteroLargo(detalle.idMedicamento(), "Seleccioná un medicamento.");
            if (!medicamentos.add(idMedicamento)) {
                throw new IllegalArgumentException(
                        "Un medicamento no puede repetirse dentro de la misma receta.");
            }
            detalles.add(new DetalleNormalizado(
                    idMedicamento,
                    textoObligatorio(detalle.dosis(), "Indicá la dosis.", 80),
                    textoObligatorio(detalle.frecuencia(), "Indicá la frecuencia.", 80),
                    enteroOpcional(detalle.duracionDias(), "La duración", 1, 3650),
                    decimalPositivo(detalle.cantidad(), "La cantidad"),
                    textoObligatorio(detalle.unidad(), "Indicá la unidad.", 30),
                    textoOpcional(detalle.instrucciones(), 2000, "Las instrucciones")
            ));
        }
        return new DatosNormalizados(vencimiento, indicaciones, detalles);
    }

    private static LocalDate fechaOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            if (fecha.isBefore(LocalDate.now(DisponibilidadService.ZONA_HORARIA))) {
                throw new IllegalArgumentException(
                        "La fecha de vencimiento no puede estar en el pasado.");
            }
            return fecha;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha de vencimiento no es válida.", error);
        }
    }

    private static long enteroLargo(String valor, String mensaje) {
        try {
            long numero = Long.parseLong(valor);
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensaje, error);
        }
    }

    private static Integer enteroOpcional(
            String valor, String campo, int minimo, int maximo
    ) {
        if (valor == null || valor.isBlank()) return null;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < minimo || numero > maximo) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    campo + " debe estar entre " + minimo + " y " + maximo + " días.", error);
        }
    }

    private static BigDecimal decimalPositivo(String valor, String campo) {
        try {
            BigDecimal numero = new BigDecimal(valor.strip().replace(',', '.'))
                    .setScale(2, RoundingMode.UNNECESSARY);
            if (numero.signum() <= 0 || numero.precision() > 10) throw new ArithmeticException();
            return numero;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    campo + " debe ser un número positivo con hasta dos decimales.", error);
        }
    }

    private static String textoObligatorio(String valor, String mensaje, int maximo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("El campo no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static String textoOpcional(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(campo + " no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static void propagar(Exception error, String mensaje) throws SQLException {
        if (error instanceof IllegalArgumentException validacion) throw validacion;
        if (error instanceof SQLException sql) throw sql;
        throw new SQLException(mensaje, error);
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException error) {
            original.addSuppressed(error);
        }
    }

    private record DatosNormalizados(
            LocalDate fechaVencimiento,
            String indicacionesGenerales,
            List<DetalleNormalizado> detalles
    ) {
    }

    private record DetalleNormalizado(
            long idMedicamento,
            String dosis,
            String frecuencia,
            Integer duracionDias,
            BigDecimal cantidad,
            String unidad,
            String instrucciones
    ) {
    }
}

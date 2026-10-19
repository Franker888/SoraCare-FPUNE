package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.AgendaMedicoDAO;
import py.fpune.soracare.dao.AtencionMedicaDAO;
import py.fpune.soracare.dao.DocumentoClinicoDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.PacienteDAO;
import py.fpune.soracare.dto.ArchivoDocumentoClinicoDTO;
import py.fpune.soracare.dto.AtencionMedicaDTO;
import py.fpune.soracare.dto.DocumentoClinicoDTO;
import py.fpune.soracare.dto.RegistroDocumentoClinicoDTO;
import py.fpune.soracare.dto.TipoDocumentoClinicoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.Paciente;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class DocumentoClinicoService {

    private static final int LIMITE_DESCRIPCION = 500;

    private final DocumentoClinicoDAO documentoDAO = new DocumentoClinicoDAO();
    private final PacienteDAO pacienteDAO = new PacienteDAO();
    private final MedicoDAO medicoDAO = new MedicoDAO();
    private final AgendaMedicoDAO agendaMedicoDAO = new AgendaMedicoDAO();
    private final AtencionMedicaDAO atencionMedicaDAO = new AtencionMedicaDAO();
    private final AlmacenDocumentosClinicos almacen = new AlmacenDocumentosClinicos();

    public List<TipoDocumentoClinicoDTO> listarTipos() throws SQLException {
        return documentoDAO.listarTiposActivos();
    }

    public List<DocumentoClinicoDTO> listarParaPaciente(
            UsuarioAutenticadoDTO usuario
    ) throws SQLException {
        Paciente paciente = pacienteAutenticado(usuario);
        return documentoDAO.listarPorPaciente(paciente.getIdPaciente());
    }

    public List<DocumentoClinicoDTO> listarParaMedico(
            UsuarioAutenticadoDTO usuario,
            long idCita
    ) throws SQLException {
        long idPaciente = pacienteAutorizadoParaMedico(usuario, idCita);
        return documentoDAO.listarPorPaciente(idPaciente);
    }

    public long cargarPorPaciente(
            UsuarioAutenticadoDTO usuario,
            short idTipoDocumento,
            String fechaDocumento,
            String descripcion,
            String nombreOriginal,
            byte[] contenido
    ) throws SQLException, IOException {
        Paciente paciente = pacienteAutenticado(usuario);
        return cargar(
                paciente.getIdPaciente(),
                null,
                usuario.getIdUsuario(),
                idTipoDocumento,
                fechaDocumento,
                descripcion,
                nombreOriginal,
                contenido
        );
    }

    public long cargarPorMedico(
            UsuarioAutenticadoDTO usuario,
            long idCita,
            short idTipoDocumento,
            String fechaDocumento,
            String descripcion,
            String nombreOriginal,
            byte[] contenido
    ) throws SQLException, IOException {
        long idPaciente = pacienteAutorizadoParaMedico(usuario, idCita);
        AtencionMedicaDTO atencion = atencionMedicaDAO.buscarPorCita(idCita).orElse(null);
        Long idAtencion = atencion == null ? null : atencion.idAtencion();
        return cargar(
                idPaciente,
                idAtencion,
                usuario.getIdUsuario(),
                idTipoDocumento,
                fechaDocumento,
                descripcion,
                nombreOriginal,
                contenido
        );
    }

    public DocumentoDescargable abrir(
            UsuarioAutenticadoDTO usuario,
            long idDocumento,
            Long idCita,
            boolean descargar,
            String direccionIp,
            String agenteUsuario
    ) throws SQLException, IOException {
        if (idDocumento <= 0) {
            throw new IllegalArgumentException("El documento seleccionado no es válido.");
        }
        ArchivoDocumentoClinicoDTO archivo = documentoDAO.buscarArchivo(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El documento clínico no existe o ya no está disponible."));
        boolean autorizado = false;
        try {
            if (usuario.tieneRol("PACIENTE")) {
                autorizado = pacienteAutenticado(usuario).getIdPaciente()
                        .equals(archivo.idPaciente());
            } else if (usuario.tieneRol("MEDICO") && idCita != null) {
                autorizado = pacienteAutorizadoParaMedico(usuario, idCita)
                        == archivo.idPaciente();
            }
            if (!autorizado) {
                throw new SecurityException(
                        "No tenés autorización para consultar este documento clínico.");
            }
            byte[] contenido = archivo.contenido() == null
                    ? almacen.leer(archivo.rutaAlmacenamiento(), archivo.hashSha256())
                    : almacen.verificarContenido(archivo.contenido(), archivo.hashSha256());
            documentoDAO.registrarAcceso(
                    idDocumento,
                    usuario.getIdUsuario(),
                    descargar ? "DESCARGAR" : "VISUALIZAR",
                    direccionIp,
                    agenteUsuario,
                    true,
                    null
            );
            return new DocumentoDescargable(
                    archivo.nombreOriginal(),
                    archivo.tipoMime(),
                    contenido
            );
        } catch (Exception error) {
            try {
                documentoDAO.registrarAcceso(
                        idDocumento,
                        usuario.getIdUsuario(),
                        descargar ? "DESCARGAR" : "VISUALIZAR",
                        direccionIp,
                        agenteUsuario,
                        false,
                        mensajeSeguro(error)
                );
            } catch (SQLException auditoria) {
                error.addSuppressed(auditoria);
            }
            if (error instanceof SecurityException seguridad) throw seguridad;
            if (error instanceof IllegalArgumentException validacion) throw validacion;
            if (error instanceof SQLException sql) throw sql;
            if (error instanceof IOException io) throw io;
            throw new IOException("No fue posible abrir el documento clínico.", error);
        }
    }

    private long cargar(
            long idPaciente,
            Long idAtencion,
            long idUsuario,
            short idTipoDocumento,
            String fechaDocumento,
            String descripcion,
            String nombreOriginal,
            byte[] contenido
    ) throws SQLException, IOException {
        LocalDate fecha = convertirFecha(fechaDocumento);
        String descripcionNormalizada = normalizarDescripcion(descripcion);
        AlmacenDocumentosClinicos.ArchivoPreparado preparado =
                almacen.preparar(idPaciente, nombreOriginal, contenido);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                documentoDAO.buscarTipoActivo(conexion, idTipoDocumento)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El tipo de documento clínico no es válido."));
                long idDocumento = documentoDAO.registrar(
                        conexion,
                        new RegistroDocumentoClinicoDTO(
                                idPaciente,
                                idAtencion,
                                idTipoDocumento,
                                idUsuario,
                                preparado.nombreAlmacenado(),
                                preparado.nombreOriginal(),
                                preparado.extension(),
                                preparado.tipoMime(),
                                preparado.tamanoBytes(),
                                null,
                                contenido,
                                preparado.hashSha256(),
                                descripcionNormalizada,
                                fecha
                        )
                );
                conexion.commit();
                return idDocumento;
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) throw sql;
                if (error instanceof IOException io) throw io;
                throw new SQLException("No fue posible registrar el documento clínico.", error);
            }
        }
    }

    private Paciente pacienteAutenticado(UsuarioAutenticadoDTO usuario)
            throws SQLException {
        validarUsuario(usuario);
        if (!usuario.tieneRol("PACIENTE")) {
            throw new SecurityException("Esta operación corresponde al paciente.");
        }
        return pacienteDAO.buscarPorIdPersona(usuario.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no posee una ficha de paciente asociada."));
    }

    private long pacienteAutorizadoParaMedico(
            UsuarioAutenticadoDTO usuario,
            long idCita
    ) throws SQLException {
        validarUsuario(usuario);
        if (!usuario.tieneRol("MEDICO") || idCita <= 0) {
            throw new SecurityException("No existe autorización médica para esta operación.");
        }
        Medico medico = medicoDAO.buscarPorIdPersona(usuario.getIdPersona())
                .orElseThrow(() -> new SecurityException(
                        "La cuenta no está vinculada con un médico registrado."));
        return agendaMedicoDAO.buscarPorCitaYMedico(idCita, medico.getIdMedico())
                .orElseThrow(() -> new SecurityException(
                        "La cita no pertenece al médico autenticado."))
                .idPaciente();
    }

    private static LocalDate convertirFecha(String valor) {
        LocalDate hoy = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        if (valor == null || valor.isBlank()) return hoy;
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            if (fecha.isAfter(hoy)) {
                throw new IllegalArgumentException(
                        "La fecha del estudio no puede ser futura.");
            }
            return fecha;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La fecha del estudio no es válida.", error);
        }
    }

    private static String normalizarDescripcion(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.strip();
        if (texto.length() > LIMITE_DESCRIPCION) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar " + LIMITE_DESCRIPCION + " caracteres.");
        }
        return texto;
    }

    private static void validarUsuario(UsuarioAutenticadoDTO usuario) {
        if (usuario == null || usuario.getIdUsuario() <= 0 || usuario.getIdPersona() <= 0) {
            throw new SecurityException("No existe una sesión válida.");
        }
    }

    private static String mensajeSeguro(Exception error) {
        String mensaje = error.getMessage();
        if (mensaje == null || mensaje.isBlank()) return "Acceso rechazado.";
        String limpio = mensaje.strip();
        return limpio.substring(0, Math.min(limpio.length(), 500));
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    public record DocumentoDescargable(
            String nombreOriginal,
            String tipoMime,
            byte[] contenido
    ) {
    }
}

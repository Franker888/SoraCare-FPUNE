package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.SolicitudSifenDAO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.dto.EstadoPreparacionSifenDTO;
import py.fpune.soracare.sifen.ClienteConsultaSifenService;
import py.fpune.soracare.sifen.ResultadoConsultaSifen;
import java.sql.SQLException;
import java.util.Arrays;

/** Consulta diagnostica: no sobrescribe respuestas de emision ni modifica estados. */
public class ConsultaSifenService {
    private final SolicitudSifenDAO dao;
    private final ConfiguracionSifenService configuracion;
    private final ClienteConsultaSifenService cliente;
    private final py.fpune.soracare.dao.ConsultaSifenDAO historial;
    public ConsultaSifenService() { this(new SolicitudSifenDAO(), new ConfiguracionSifenService(), new ClienteConsultaSifenService()); }
    ConsultaSifenService(SolicitudSifenDAO dao, ConfiguracionSifenService configuracion, ClienteConsultaSifenService cliente) {
        this(dao, configuracion, cliente, new py.fpune.soracare.dao.ConsultaSifenDAO());
    }
    ConsultaSifenService(SolicitudSifenDAO dao, ConfiguracionSifenService configuracion, ClienteConsultaSifenService cliente,
            py.fpune.soracare.dao.ConsultaSifenDAO historial) {
        this.dao = dao; this.configuracion = configuracion; this.cliente = cliente;
        this.historial = historial;
    }
    public ResultadoConsultaSifen consultar(long idSolicitud, UsuarioAutenticadoDTO usuario) throws SQLException {
        if (idSolicitud <= 0 || usuario == null || !usuario.tieneRol("MEDICO") || usuario.isDebeCambiarContrasena()) {
            throw new IllegalArgumentException("No tiene permiso para consultar este documento.");
        }
        var documento = dao.buscarKudeDelMedico(idSolicitud, usuario.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException("No tiene permiso para consultar este documento."));
        if (!"PRUEBA".equals(documento.ambiente()) || documento.cdc() == null || !documento.cdc().matches("[0-9]{44}")) {
            throw new IllegalArgumentException("Solo se permite consultar un CDC propio del ambiente PRUEBA.");
        }
        try (var cfg = configuracion.cargarConfiguracion()) {
            if (cfg.ambiente() != EstadoPreparacionSifenDTO.Ambiente.PRUEBA) throw new IllegalArgumentException("Consulta limitada a PRUEBA.");
            long identificador = siguienteIdentificador();
            historial.iniciar(identificador, idSolicitud, usuario.getIdPersona(), documento.cdc());
            char[] password = cfg.passwordCertificado();
            try {
                var resultado = cliente.consultarPrueba(identificador, documento.cdc(), cfg.certificado(), password);
                historial.finalizar(identificador, resultado);
                return resultado;
            }
            finally { Arrays.fill(password, '\0'); }
        }
    }
    public java.util.List<py.fpune.soracare.dto.ConsultaSifenHistorialDTO> historial(UsuarioAutenticadoDTO usuario) throws SQLException {
        if (usuario == null || !usuario.tieneRol("MEDICO") || usuario.isDebeCambiarContrasena())
            throw new IllegalArgumentException("No tiene permiso para consultar el historial.");
        return historial.listarDelMedico(usuario.getIdPersona());
    }
    long siguienteIdentificador() throws SQLException {
        try (var conexion = ConexionBD.obtenerConexion()) { return dao.siguienteIdentificadorEnvio(conexion); }
    }
}

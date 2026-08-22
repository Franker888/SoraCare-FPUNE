package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.EstadoSistemaDAO;
import py.fpune.soracare.dto.EstadoSistemaDTO;
import py.fpune.soracare.dto.ResumenPanelAdministracionDTO;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;

public class EstadoSistemaService {

    private final EstadoSistemaDAO estadoDAO = new EstadoSistemaDAO();

    public EstadoSistemaDTO consultar() throws SQLException {
        EstadoSistemaDAO.DatosBaseDatos baseDatos = estadoDAO.consultarEstado();
        Runtime runtime = Runtime.getRuntime();
        long memoriaUsada = runtime.totalMemory() - runtime.freeMemory();
        return new EstadoSistemaDTO(
                true,
                baseDatos.hora(),
                baseDatos.version(),
                baseDatos.usuariosActivos(),
                baseDatos.citasFuturas(),
                baseDatos.eventosUltimas24Horas(),
                ConexionBD.obtenerConexionesCreadas(),
                ConexionBD.obtenerConexionesDisponibles(),
                ConexionBD.obtenerMaximoConexiones(),
                memoriaUsada / (1024 * 1024),
                runtime.maxMemory() / (1024 * 1024),
                runtime.availableProcessors(),
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
                System.getProperty("java.version")
        );
    }

    public ResumenPanelAdministracionDTO consultarResumenPanel() throws SQLException {
        return estadoDAO.consultarResumenPanel();
    }
}

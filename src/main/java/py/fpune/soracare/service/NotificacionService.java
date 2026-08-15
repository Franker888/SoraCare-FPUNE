package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.NotificacionDAO;
import py.fpune.soracare.dto.NotificacionDTO;
import py.fpune.soracare.mvc.model.Paciente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class NotificacionService {

    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final PacienteService pacienteService = new PacienteService();

    public List<NotificacionDTO> listarParaPaciente(long idPersona) throws SQLException {
        validarPaciente(idPersona);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return notificacionDAO.listarInternasPorPersona(conexion, idPersona);
        }
    }

    public long contarNoLeidasParaPaciente(long idPersona) throws SQLException {
        validarPaciente(idPersona);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return notificacionDAO.contarInternasNoLeidas(conexion, idPersona);
        }
    }

    public void marcarLeida(long idPersona, long idNotificacion) throws SQLException {
        validarPaciente(idPersona);
        if (idNotificacion <= 0) {
            throw new IllegalArgumentException("La notificación seleccionada no es válida.");
        }
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            notificacionDAO.marcarLeida(conexion, idNotificacion, idPersona);
        }
    }

    public void marcarTodasLeidas(long idPersona) throws SQLException {
        validarPaciente(idPersona);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            notificacionDAO.marcarTodasLeidas(conexion, idPersona);
        }
    }

    private Paciente validarPaciente(long idPersona) throws SQLException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La sesión del paciente no es válida.");
        }
        return pacienteService.buscarPorIdPersona(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no posee una ficha de paciente asociada."));
    }
}

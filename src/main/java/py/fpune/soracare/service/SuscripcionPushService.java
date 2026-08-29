package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.SuscripcionPushDAO;

import java.sql.Connection;
import java.sql.SQLException;

public class SuscripcionPushService {

    private static final int LONGITUD_MAXIMA_TOKEN = 4096;
    private static final int LONGITUD_MAXIMA_USER_AGENT = 500;
    private final SuscripcionPushDAO suscripcionPushDAO;

    public SuscripcionPushService() {
        this(new SuscripcionPushDAO());
    }

    SuscripcionPushService(SuscripcionPushDAO suscripcionPushDAO) {
        this.suscripcionPushDAO = suscripcionPushDAO;
    }

    public void registrar(long idUsuario, String token, String userAgent) throws SQLException {
        validarIdUsuario(idUsuario);
        String tokenNormalizado = normalizarToken(token);
        String agenteNormalizado = normalizarUserAgent(userAgent);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            suscripcionPushDAO.registrarOActualizar(
                    conexion, idUsuario, tokenNormalizado, agenteNormalizado);
        }
    }

    public void desactivar(long idUsuario, String token) throws SQLException {
        validarIdUsuario(idUsuario);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            suscripcionPushDAO.desactivar(conexion, idUsuario, normalizarToken(token));
        }
    }

    public int contarActivas(long idUsuario) throws SQLException {
        validarIdUsuario(idUsuario);
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return suscripcionPushDAO.contarActivas(conexion, idUsuario);
        }
    }

    private static void validarIdUsuario(long idUsuario) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException("El usuario de la suscripción no es válido.");
        }
    }

    private static String normalizarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("No se recibió el identificador del dispositivo.");
        }
        String normalizado = token.strip();
        if (normalizado.length() > LONGITUD_MAXIMA_TOKEN) {
            throw new IllegalArgumentException("El identificador del dispositivo es demasiado largo.");
        }
        return normalizado;
    }

    private static String normalizarUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalizado = userAgent.strip();
        return normalizado.length() <= LONGITUD_MAXIMA_USER_AGENT
                ? normalizado
                : normalizado.substring(0, LONGITUD_MAXIMA_USER_AGENT);
    }
}

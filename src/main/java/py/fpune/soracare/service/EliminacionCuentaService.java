package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.EliminacionCuentaDAO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class EliminacionCuentaService {

    private final EliminacionCuentaDAO eliminacionCuentaDAO;

    public EliminacionCuentaService() {
        eliminacionCuentaDAO = new EliminacionCuentaDAO();
    }

    public void eliminarCuenta(
            UsuarioAutenticadoDTO usuario
    ) throws SQLException {

        validarPaciente(usuario);

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);

            try {
                if (eliminacionCuentaDAO.tieneCitasFuturasActivas(
                        conexion,
                        usuario.getIdPersona()
                )) {
                    throw new IllegalStateException(
                            "Antes de eliminar tu cuenta, cancelá o completá "
                                    + "todas tus citas futuras activas."
                    );
                }

                eliminacionCuentaDAO.eliminarIdentidadesExternas(
                        conexion,
                        usuario.getIdUsuario()
                );

                boolean cuentaInactivada =
                        eliminacionCuentaDAO.inactivarUsuario(
                                conexion,
                                usuario.getIdUsuario(),
                                usuario.getIdPersona()
                        );

                if (!cuentaInactivada) {
                    throw new IllegalStateException(
                            "La cuenta ya no se encuentra activa."
                    );
                }

                conexion.commit();
            } catch (SQLException | RuntimeException error) {
                intentarRollback(conexion, error);
                throw error;
            }
        }
    }

    private static void validarPaciente(UsuarioAutenticadoDTO usuario) {
        if (usuario == null) {
            throw new SecurityException("Debés iniciar sesión nuevamente.");
        }

        Set<String> roles = usuario.getRoles();
        if (!roles.contains("PACIENTE") || roles.size() != 1) {
            throw new SecurityException(
                    "Esta opción está disponible únicamente para cuentas de pacientes."
            );
        }
    }

    private static void intentarRollback(
            Connection conexion,
            Exception errorOriginal
    ) {
        try {
            conexion.rollback();
        } catch (SQLException errorRollback) {
            errorOriginal.addSuppressed(errorRollback);
        }
    }
}

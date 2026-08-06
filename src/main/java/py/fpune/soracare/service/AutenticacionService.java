package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.UsuarioDAO;
import py.fpune.soracare.dto.CredencialesAutenticacionDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.EstadoUsuario;
import py.fpune.soracare.mvc.model.Rol;
import py.fpune.soracare.mvc.model.Usuario;
import py.fpune.soracare.security.CodificadorContrasenaBCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class AutenticacionService {

    private static final short MAXIMO_INTENTOS_FALLIDOS = 5;

    private static final int MINUTOS_BLOQUEO = 15;

    private static final String MENSAJE_CREDENCIALES_INVALIDAS =
            "No fue posible iniciar sesión con las credenciales proporcionadas.";

    private static final String MENSAJE_CUENTA_BLOQUEADA =
            "La cuenta está temporalmente bloqueada. "
                    + "Intentá nuevamente más tarde.";

    private final UsuarioDAO usuarioDAO;

    public AutenticacionService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    public UsuarioAutenticadoDTO autenticar(
            CredencialesAutenticacionDTO credenciales
    ) throws SQLException {

        validarCredenciales(
                credenciales
        );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(false);

            boolean transaccionFinalizada = false;

            try {
                Usuario usuario =
                        buscarUsuario(
                                conexion,
                                credenciales.getIdentificador()
                        );

                usuario =
                        procesarBloqueoSiCorresponde(
                                conexion,
                                usuario,
                                credenciales.getIdentificador()
                        );

                validarEstadoCuenta(
                        usuario
                );

                boolean contrasenaCorrecta =
                        CodificadorContrasenaBCrypt.verificar(
                                credenciales.getContrasena(),
                                usuario.getContrasenaHash()
                        );

                if (!contrasenaCorrecta) {
                    usuarioDAO.registrarIntentoFallido(
                            conexion,
                            usuario.getIdUsuario(),
                            MAXIMO_INTENTOS_FALLIDOS,
                            MINUTOS_BLOQUEO
                    );

                    Usuario usuarioActualizado =
                            buscarUsuario(
                                    conexion,
                                    credenciales.getIdentificador()
                            );

                    conexion.commit();
                    transaccionFinalizada = true;

                    if (usuarioActualizado.getEstado()
                            == EstadoUsuario.BLOQUEADO) {

                        throw new SecurityException(
                                MENSAJE_CUENTA_BLOQUEADA
                        );
                    }

                    throw new SecurityException(
                            MENSAJE_CREDENCIALES_INVALIDAS
                    );
                }

                validarRolesActivos(
                        usuario
                );

                usuarioDAO.registrarAccesoExitoso(
                        conexion,
                        usuario.getIdUsuario()
                );

                UsuarioAutenticadoDTO usuarioAutenticado =
                        convertirAUsuarioAutenticado(
                                usuario
                        );

                conexion.commit();
                transaccionFinalizada = true;

                return usuarioAutenticado;

            } catch (
                    SQLException
                    | RuntimeException error
            ) {
                if (!transaccionFinalizada) {
                    ejecutarRollback(
                            conexion,
                            error
                    );
                }

                throw error;
            }
        }
    }

    private Usuario buscarUsuario(
            Connection conexion,
            String identificador
    ) throws SQLException {

        Optional<Usuario> resultado =
                usuarioDAO.buscarPorIdentificador(
                        conexion,
                        identificador
                );

        if (resultado.isEmpty()) {
            throw new SecurityException(
                    MENSAJE_CREDENCIALES_INVALIDAS
            );
        }

        return resultado.get();
    }

    private Usuario procesarBloqueoSiCorresponde(
            Connection conexion,
            Usuario usuario,
            String identificador
    ) throws SQLException {

        if (usuario.getEstado()
                != EstadoUsuario.BLOQUEADO) {

            return usuario;
        }

        boolean bloqueoExpirado =
                usuarioDAO.desbloquearSiBloqueoExpiro(
                        conexion,
                        usuario.getIdUsuario()
                );

        if (!bloqueoExpirado) {
            throw new SecurityException(
                    MENSAJE_CUENTA_BLOQUEADA
            );
        }

        return buscarUsuario(
                conexion,
                identificador
        );
    }

    private static void validarEstadoCuenta(
            Usuario usuario
    ) {
        if (usuario.getEstado()
                == EstadoUsuario.INACTIVO) {

            throw new SecurityException(
                    MENSAJE_CREDENCIALES_INVALIDAS
            );
        }

        if (usuario.getEstado()
                == EstadoUsuario.BLOQUEADO) {

            throw new SecurityException(
                    MENSAJE_CUENTA_BLOQUEADA
            );
        }

        if (usuario.getEstado()
                != EstadoUsuario.ACTIVO) {

            throw new SecurityException(
                    MENSAJE_CREDENCIALES_INVALIDAS
            );
        }
    }

    private static void validarRolesActivos(
            Usuario usuario
    ) {
        if (usuario.getRoles() == null
                || usuario.getRoles().isEmpty()) {

            throw new SecurityException(
                    "La cuenta no posee un rol activo autorizado."
            );
        }
    }

    private static UsuarioAutenticadoDTO convertirAUsuarioAutenticado(
            Usuario usuario
    ) {
        if (usuario.getIdUsuario() == null
                || usuario.getIdUsuario() <= 0) {

            throw new IllegalStateException(
                    "El usuario autenticado no posee un ID válido."
            );
        }

        if (usuario.getIdPersona() == null
                || usuario.getIdPersona() <= 0) {

            throw new IllegalStateException(
                    "El usuario autenticado no está relacionado "
                            + "con una persona válida."
            );
        }

        Set<String> nombresRoles =
                new LinkedHashSet<>();

        for (Rol rol : usuario.getRoles()) {
            if (rol == null
                    || rol.getNombre() == null
                    || rol.getNombre().isBlank()) {

                continue;
            }

            nombresRoles.add(
                    rol.getNombre()
            );
        }

        if (nombresRoles.isEmpty()) {
            throw new SecurityException(
                    "La cuenta no posee un rol activo autorizado."
            );
        }

        return new UsuarioAutenticadoDTO(
                usuario.getIdUsuario(),
                usuario.getIdPersona(),
                usuario.getNombreUsuario(),
                usuario.isDebeCambiarContrasena(),
                nombresRoles
        );
    }

    private static void validarCredenciales(
            CredencialesAutenticacionDTO credenciales
    ) {
        if (credenciales == null) {
            throw new IllegalArgumentException(
                    "Las credenciales de autenticación "
                            + "son obligatorias."
            );
        }

        if (credenciales.getIdentificador() == null
                || credenciales
                .getIdentificador()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de usuario o correo "
                            + "es obligatorio."
            );
        }

        if (credenciales.getContrasena() == null
                || credenciales
                .getContrasena()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "La contraseña es obligatoria."
            );
        }

        if (credenciales
                .getIdentificador()
                .strip()
                .length() > 150) {

            throw new IllegalArgumentException(
                    "El identificador ingresado es demasiado largo."
            );
        }

        if (credenciales
                .getContrasena()
                .length() > 72) {

            throw new IllegalArgumentException(
                    "La contraseña ingresada es demasiado larga."
            );
        }
    }

    private static void ejecutarRollback(
            Connection conexion,
            Exception errorOriginal
    ) {
        try {
            conexion.rollback();

        } catch (SQLException errorRollback) {
            errorOriginal.addSuppressed(
                    errorRollback
            );
        }
    }
}
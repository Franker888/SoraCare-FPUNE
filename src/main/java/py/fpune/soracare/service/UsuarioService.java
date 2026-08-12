package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.PersonaDAO;
import py.fpune.soracare.dao.RolDAO;
import py.fpune.soracare.dao.UsuarioDAO;
import py.fpune.soracare.dto.SolicitudCreacionUsuarioDTO;
import py.fpune.soracare.mvc.model.EstadoUsuario;
import py.fpune.soracare.mvc.model.Rol;
import py.fpune.soracare.mvc.model.Usuario;
import py.fpune.soracare.security.CodificadorContrasenaBCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class UsuarioService {

    private static final int LONGITUD_MINIMA_CONTRASENA = 10;
    private static final int LONGITUD_MAXIMA_CONTRASENA = 72;

    private final PersonaDAO personaDAO =
            new PersonaDAO();

    private final RolDAO rolDAO =
            new RolDAO();

    private final UsuarioDAO usuarioDAO =
            new UsuarioDAO();

    public Usuario crearUsuario(
            SolicitudCreacionUsuarioDTO solicitud
    ) throws SQLException {

        return crearUsuario(
                solicitud,
                null
        );
    }

    public Set<Long> listarIdsPersonasConCuenta() throws SQLException {
        return usuarioDAO.listarIdsPersonasConCuenta();
    }

    public Usuario crearUsuario(
            SolicitudCreacionUsuarioDTO solicitud,
            Long idUsuarioAsignador
    ) throws SQLException {

        if (solicitud == null) {
            throw new SQLException(
                    "La solicitud de creación no puede ser nula."
            );
        }

        long idPersona =
                convertirIdPersona(
                        solicitud.getIdPersona()
                );

        String nombreUsuario =
                normalizarNombreUsuario(
                        solicitud.getNombreUsuario()
                );

        validarContrasenas(
                solicitud.getContrasena(),
                solicitud.getConfirmacionContrasena()
        );

        String nombreRol =
                normalizarNombreRol(
                        solicitud.getNombreRol()
                );

        if (idUsuarioAsignador != null
                && idUsuarioAsignador <= 0) {

            throw new SQLException(
                    "El ID del usuario asignador no es válido."
            );
        }

        String contrasenaHash =
                CodificadorContrasenaBCrypt.codificar(
                        solicitud.getContrasena()
                );

        Usuario usuario =
                new Usuario();

        usuario.setIdPersona(
                idPersona
        );

        usuario.setNombreUsuario(
                nombreUsuario
        );

        usuario.setContrasenaHash(
                contrasenaHash
        );

        usuario.setEstado(
                EstadoUsuario.ACTIVO
        );

        usuario.setIntentosFallidos(
                (short) 0
        );

        usuario.setDebeCambiarContrasena(
                solicitud.isDebeCambiarContrasena()
        );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            try {
                conexion.setAutoCommit(false);

                if (!personaDAO.existePorId(
                        conexion,
                        idPersona
                )) {
                    throw new SQLException(
                            "La persona indicada no existe."
                    );
                }

                if (usuarioDAO.existeParaPersona(
                        conexion,
                        idPersona
                )) {
                    throw new SQLException(
                            "La persona ya tiene una cuenta de usuario."
                    );
                }

                if (usuarioDAO.existePorNombreUsuario(
                        conexion,
                        nombreUsuario
                )) {
                    throw new SQLException(
                            "El nombre de usuario ya está registrado."
                    );
                }

                Optional<Rol> rolEncontrado =
                        rolDAO.buscarActivoPorNombre(
                                conexion,
                                nombreRol
                        );

                if (rolEncontrado.isEmpty()) {
                    throw new SQLException(
                            "El rol indicado no existe o está inactivo."
                    );
                }

                Rol rol =
                        rolEncontrado.get();

                long idUsuario =
                        usuarioDAO.registrar(
                                conexion,
                                usuario
                        );

                usuarioDAO.asignarRol(
                        conexion,
                        idUsuario,
                        rol.getIdRol(),
                        idUsuarioAsignador
                );

                usuario.agregarRol(
                        rol
                );

                conexion.commit();

                return usuario;

            } catch (SQLException error) {

                try {
                    conexion.rollback();

                } catch (SQLException errorRollback) {
                    error.addSuppressed(
                            errorRollback
                    );
                }

                throw error;
            }
        }
    }

    private static long convertirIdPersona(
            String idPersona
    ) throws SQLException {

        if (idPersona == null
                || idPersona.isBlank()) {

            throw new SQLException(
                    "El ID de la persona es obligatorio."
            );
        }

        String idLimpio =
                idPersona.strip();

        if (!idLimpio.matches("\\d+")) {
            throw new SQLException(
                    "El ID de la persona debe contener solamente números."
            );
        }

        try {
            long idNumerico =
                    Long.parseLong(
                            idLimpio
                    );

            if (idNumerico <= 0) {
                throw new SQLException(
                        "El ID de la persona debe ser mayor que cero."
                );
            }

            return idNumerico;

        } catch (NumberFormatException error) {
            throw new SQLException(
                    "El ID de la persona no es válido.",
                    error
            );
        }
    }

    private static String normalizarNombreUsuario(
            String nombreUsuario
    ) throws SQLException {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new SQLException(
                    "El nombre de usuario es obligatorio."
            );
        }

        String nombreNormalizado =
                nombreUsuario
                        .strip()
                        .toLowerCase(Locale.ROOT);

        if (nombreNormalizado.length() < 4
                || nombreNormalizado.length() > 50) {

            throw new SQLException(
                    "El nombre de usuario debe tener entre 4 y 50 caracteres."
            );
        }

        if (!nombreNormalizado.matches(
                "[a-z0-9._-]+"
        )) {
            throw new SQLException(
                    "El nombre de usuario solamente puede contener letras, "
                            + "números, puntos, guiones y guiones bajos."
            );
        }

        return nombreNormalizado;
    }

    private static String normalizarNombreRol(
            String nombreRol
    ) throws SQLException {

        if (nombreRol == null
                || nombreRol.isBlank()) {

            throw new SQLException(
                    "El rol es obligatorio."
            );
        }

        return nombreRol
                .strip()
                .toUpperCase(Locale.ROOT);
    }

    private static void validarContrasenas(
            String contrasena,
            String confirmacionContrasena
    ) throws SQLException {

        if (contrasena == null
                || contrasena.isBlank()) {

            throw new SQLException(
                    "La contraseña es obligatoria."
            );
        }

        if (confirmacionContrasena == null
                || confirmacionContrasena.isBlank()) {

            throw new SQLException(
                    "Debe confirmar la contraseña."
            );
        }

        if (!contrasena.equals(
                confirmacionContrasena
        )) {
            throw new SQLException(
                    "Las contraseñas no coinciden."
            );
        }

        if (contrasena.length()
                < LONGITUD_MINIMA_CONTRASENA) {

            throw new SQLException(
                    "La contraseña debe tener como mínimo "
                            + LONGITUD_MINIMA_CONTRASENA
                            + " caracteres."
            );
        }

        if (contrasena.length()
                > LONGITUD_MAXIMA_CONTRASENA) {

            throw new SQLException(
                    "La contraseña no puede superar "
                            + LONGITUD_MAXIMA_CONTRASENA
                            + " caracteres."
            );
        }

        if (!contrasena.matches(".*[A-Z].*")) {
            throw new SQLException(
                    "La contraseña debe contener al menos una letra mayúscula."
            );
        }

        if (!contrasena.matches(".*[a-z].*")) {
            throw new SQLException(
                    "La contraseña debe contener al menos una letra minúscula."
            );
        }

        if (!contrasena.matches(".*\\d.*")) {
            throw new SQLException(
                    "La contraseña debe contener al menos un número."
            );
        }

        if (!contrasena.matches(
                ".*[^A-Za-z0-9].*"
        )) {
            throw new SQLException(
                    "La contraseña debe contener al menos un carácter especial."
            );
        }
    }
}

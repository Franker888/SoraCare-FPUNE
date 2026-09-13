package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.UsuarioDAO;
import py.fpune.soracare.dto.SolicitudCambioContrasenaDTO;
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

public class CambioContrasenaService {

    private static final int LONGITUD_MINIMA_CONTRASENA = 10;

    private static final int LONGITUD_MAXIMA_CONTRASENA = 72;

    private final UsuarioDAO usuarioDAO;
    private final PwnedPasswordsService pwnedPasswordsService;

    public CambioContrasenaService() {
        this(new UsuarioDAO(), new PwnedPasswordsService());
    }

    CambioContrasenaService(
            UsuarioDAO usuarioDAO,
            PwnedPasswordsService pwnedPasswordsService
    ) {
        this.usuarioDAO = usuarioDAO;
        this.pwnedPasswordsService = pwnedPasswordsService;
    }

    public UsuarioAutenticadoDTO cambiarContrasena(
            long idUsuario,
            SolicitudCambioContrasenaDTO solicitud
    ) throws SQLException {
        return cambiarContrasena(idUsuario, solicitud, null);
    }

    public UsuarioAutenticadoDTO cambiarContrasena(UsuarioAutenticadoDTO sesion,
                                                   SolicitudCambioContrasenaDTO solicitud)
            throws SQLException {
        return cambiarContrasena(sesion.getIdUsuario(), solicitud, sesion.getVersionSesion());
    }

    private UsuarioAutenticadoDTO cambiarContrasena(long idUsuario,
                                                    SolicitudCambioContrasenaDTO solicitud,
                                                    Long versionSesion) throws SQLException {

        validarIdUsuario(
                idUsuario
        );

        validarSolicitud(
                solicitud
        );

        Usuario usuario;
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            usuario = buscarUsuario(conexion, idUsuario);
            validarEstadoUsuario(usuario);
            if (versionSesion != null && versionSesion != usuario.getVersionSesion()) {
                throw new SecurityException("Iniciá sesión nuevamente.");
            }
            verificarContrasenaActual(
                    solicitud.getContrasenaActual(),
                    usuario.getContrasenaHash()
            );
            verificarContrasenaDiferente(
                    solicitud.getNuevaContrasena(),
                    usuario.getContrasenaHash()
            );
        }

        pwnedPasswordsService.validar(
                solicitud.getNuevaContrasena()
        );

        String nuevoHash = CodificadorContrasenaBCrypt.codificar(
                solicitud.getNuevaContrasena()
        );

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);

            try {

                usuarioDAO.actualizarContrasena(
                        conexion,
                        idUsuario,
                        usuario.getContrasenaHash(),
                        nuevoHash,
                        usuario.getVersionSesion()
                );

                Usuario usuarioActualizado =
                        buscarUsuario(
                                conexion,
                                idUsuario
                        );

                UsuarioAutenticadoDTO resultado =
                        convertirAUsuarioAutenticado(
                                usuarioActualizado
                        );

                conexion.commit();

                return resultado;

            } catch (
                    SQLException
                    | RuntimeException error
            ) {
                ejecutarRollback(
                        conexion,
                        error
                );

                throw error;
            }
        }
    }

    private Usuario buscarUsuario(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        Optional<Usuario> resultado =
                usuarioDAO.buscarPorId(
                        conexion,
                        idUsuario
                );

        if (resultado.isEmpty()) {
            throw new SecurityException(
                    "No fue posible identificar la cuenta autenticada."
            );
        }

        return resultado.get();
    }

    private static void validarEstadoUsuario(
            Usuario usuario
    ) {
        if (usuario.getEstado()
                != EstadoUsuario.ACTIVO) {

            throw new SecurityException(
                    "La cuenta no se encuentra habilitada "
                            + "para cambiar la contraseña."
            );
        }
    }

    private static void verificarContrasenaActual(
            String contrasenaActual,
            String hashActual
    ) {
        boolean contrasenaCorrecta =
                CodificadorContrasenaBCrypt.verificar(
                        contrasenaActual,
                        hashActual
                );

        if (!contrasenaCorrecta) {
            throw new SecurityException(
                    "La contraseña actual no es correcta."
            );
        }
    }

    private static void verificarContrasenaDiferente(
            String nuevaContrasena,
            String hashActual
    ) {
        boolean esLaMismaContrasena =
                CodificadorContrasenaBCrypt.verificar(
                        nuevaContrasena,
                        hashActual
                );

        if (esLaMismaContrasena) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe ser diferente "
                            + "de la contraseña actual."
            );
        }
    }

    private static void validarIdUsuario(
            long idUsuario
    ) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }
    }

    private static void validarSolicitud(
            SolicitudCambioContrasenaDTO solicitud
    ) {
        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud de cambio de contraseña "
                            + "es obligatoria."
            );
        }

        String contrasenaActual =
                solicitud.getContrasenaActual();

        String nuevaContrasena =
                solicitud.getNuevaContrasena();

        String confirmacion =
                solicitud.getConfirmacionNuevaContrasena();

        if (contrasenaActual == null
                || contrasenaActual.isBlank()) {

            throw new IllegalArgumentException(
                    "La contraseña actual es obligatoria."
            );
        }

        if (nuevaContrasena == null
                || nuevaContrasena.isBlank()) {

            throw new IllegalArgumentException(
                    "La nueva contraseña es obligatoria."
            );
        }

        if (confirmacion == null
                || confirmacion.isBlank()) {

            throw new IllegalArgumentException(
                    "La confirmación de la nueva contraseña "
                            + "es obligatoria."
            );
        }

        if (!nuevaContrasena.equals(
                confirmacion
        )) {
            throw new IllegalArgumentException(
                    "La nueva contraseña y su confirmación "
                            + "no coinciden."
            );
        }

        if (nuevaContrasena.length()
                < LONGITUD_MINIMA_CONTRASENA) {

            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos "
                            + LONGITUD_MINIMA_CONTRASENA
                            + " caracteres."
            );
        }

        if (nuevaContrasena.length()
                > LONGITUD_MAXIMA_CONTRASENA) {

            throw new IllegalArgumentException(
                    "La nueva contraseña no puede superar "
                            + LONGITUD_MAXIMA_CONTRASENA
                            + " caracteres."
            );
        }

        if (!contieneMayuscula(
                nuevaContrasena
        )) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe contener "
                            + "al menos una letra mayúscula."
            );
        }

        if (!contieneMinuscula(
                nuevaContrasena
        )) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe contener "
                            + "al menos una letra minúscula."
            );
        }

        if (!contieneNumero(
                nuevaContrasena
        )) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe contener "
                            + "al menos un número."
            );
        }

        if (!contieneCaracterEspecial(
                nuevaContrasena
        )) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe contener "
                            + "al menos un carácter especial."
            );
        }
    }

    private static boolean contieneMayuscula(
            String contrasena
    ) {
        return contrasena
                .chars()
                .anyMatch(
                        Character::isUpperCase
                );
    }

    private static boolean contieneMinuscula(
            String contrasena
    ) {
        return contrasena
                .chars()
                .anyMatch(
                        Character::isLowerCase
                );
    }

    private static boolean contieneNumero(
            String contrasena
    ) {
        return contrasena
                .chars()
                .anyMatch(
                        Character::isDigit
                );
    }

    private static boolean contieneCaracterEspecial(
            String contrasena
    ) {
        return contrasena
                .chars()
                .anyMatch(
                        caracter ->
                                !Character.isLetterOrDigit(
                                        caracter
                                )
                );
    }

    private static UsuarioAutenticadoDTO convertirAUsuarioAutenticado(
            Usuario usuario
    ) {
        if (usuario.getIdUsuario() == null
                || usuario.getIdUsuario() <= 0) {

            throw new IllegalStateException(
                    "El usuario no posee un ID válido."
            );
        }

        if (usuario.getIdPersona() == null
                || usuario.getIdPersona() <= 0) {

            throw new IllegalStateException(
                    "El usuario no está relacionado "
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
                nombresRoles,
                usuario.getVersionSesion()
        );
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

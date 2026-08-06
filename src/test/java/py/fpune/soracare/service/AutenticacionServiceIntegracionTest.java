package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.CredencialesAutenticacionDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.EstadoUsuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;

public class AutenticacionServiceIntegracionTest {

    private static final String NOMBRE_USUARIO =
            "prueba.seguridad";

    private static final String CORREO =
            "prueba.usuario.seguridad@soracare.test";

    private static final String CONTRASENA_CORRECTA =
            "SoraCare-Prueba-2026!";

    private static final String CONTRASENA_INCORRECTA =
            "Contrasena-Incorrecta-2026!";

    private static final String SQL_OBTENER_ESTADO = """
            SELECT
                id_usuario,
                estado,
                intentos_fallidos,
                bloqueado_hasta,
                ultimo_acceso,
                actualizado_en
            FROM soracare.usuarios
            WHERE nombre_usuario = ?
            """;

    private static final String SQL_PREPARAR_CUENTA_ACTIVA = """
            UPDATE soracare.usuarios
            SET
                estado = 'ACTIVO',
                intentos_fallidos = 0,
                bloqueado_hasta = NULL,
                ultimo_acceso = NULL,
                actualizado_en = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
            """;

    private static final String SQL_EXPIRAR_BLOQUEO = """
            UPDATE soracare.usuarios
            SET
                estado = 'BLOQUEADO',
                intentos_fallidos = 5,
                bloqueado_hasta =
                    CURRENT_TIMESTAMP - INTERVAL '1 minute',
                actualizado_en = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
            """;

    private static final String SQL_RESTAURAR_ESTADO = """
            UPDATE soracare.usuarios
            SET
                estado = ?,
                intentos_fallidos = ?,
                bloqueado_hasta = ?,
                ultimo_acceso = ?,
                actualizado_en = ?
            WHERE id_usuario = ?
            """;

    public static void main(
            String[] args
    ) throws Exception {

        EstadoCuenta estadoOriginal =
                obtenerEstadoCuenta();

        try {
            prepararCuentaActiva(
                    estadoOriginal.idUsuario
            );

            probarAutenticacionPorNombreUsuario();

            prepararCuentaActiva(
                    estadoOriginal.idUsuario
            );

            probarAutenticacionPorCorreo();

            prepararCuentaActiva(
                    estadoOriginal.idUsuario
            );

            probarIntentosFallidosYBloqueo();

            probarDesbloqueoAutomatico(
                    estadoOriginal.idUsuario
            );

            prepararCuentaActiva(
                    estadoOriginal.idUsuario
            );

            probarUsuarioInexistente();

            System.out.println();
            System.out.println(
                    "AutenticacionService fue probado correctamente."
            );

            System.out.println(
                    "El login por nombre de usuario funcionó."
            );

            System.out.println(
                    "El login por correo electrónico funcionó."
            );

            System.out.println(
                    "Las contraseñas incorrectas fueron rechazadas."
            );

            System.out.println(
                    "La cuenta fue bloqueada al quinto intento."
            );

            System.out.println(
                    "El acceso durante el bloqueo fue rechazado."
            );

            System.out.println(
                    "El bloqueo vencido fue procesado correctamente."
            );

            System.out.println(
                    "El usuario inexistente fue rechazado."
            );

        } finally {
            restaurarEstadoCuenta(
                    estadoOriginal
            );

            System.out.println(
                    "El estado original de la cuenta fue restaurado."
            );
        }
    }

    private static void probarAutenticacionPorNombreUsuario()
            throws SQLException {

        AutenticacionService servicio =
                new AutenticacionService();

        CredencialesAutenticacionDTO credenciales =
                new CredencialesAutenticacionDTO(
                        NOMBRE_USUARIO,
                        CONTRASENA_CORRECTA
                );

        UsuarioAutenticadoDTO usuario =
                servicio.autenticar(
                        credenciales
                );

        validarUsuarioAutenticado(
                usuario
        );

        EstadoCuenta estado =
                obtenerEstadoCuenta();

        if (estado.intentosFallidos != 0) {
            throw new AssertionError(
                    "El acceso correcto debería conservar "
                            + "los intentos fallidos en cero."
            );
        }

        if (estado.ultimoAcceso == null) {
            throw new AssertionError(
                    "El acceso correcto debería registrar "
                            + "la fecha del último acceso."
            );
        }

        System.out.println(
                "Login por nombre de usuario: CORRECTO."
        );
    }

    private static void probarAutenticacionPorCorreo()
            throws SQLException {

        AutenticacionService servicio =
                new AutenticacionService();

        CredencialesAutenticacionDTO credenciales =
                new CredencialesAutenticacionDTO(
                        CORREO,
                        CONTRASENA_CORRECTA
                );

        UsuarioAutenticadoDTO usuario =
                servicio.autenticar(
                        credenciales
                );

        validarUsuarioAutenticado(
                usuario
        );

        System.out.println(
                "Login por correo electrónico: CORRECTO."
        );
    }

    private static void probarIntentosFallidosYBloqueo()
            throws SQLException {

        AutenticacionService servicio =
                new AutenticacionService();

        for (int intento = 1; intento <= 5; intento++) {

            CredencialesAutenticacionDTO credenciales =
                    new CredencialesAutenticacionDTO(
                            NOMBRE_USUARIO,
                            CONTRASENA_INCORRECTA
                    );

            try {
                servicio.autenticar(
                        credenciales
                );

                throw new AssertionError(
                        "La contraseña incorrecta no debería "
                                + "permitir el inicio de sesión."
                );

            } catch (SecurityException esperado) {

                EstadoCuenta estado =
                        obtenerEstadoCuenta();

                if (estado.intentosFallidos != intento) {
                    throw new AssertionError(
                            "El intento fallido número "
                                    + intento
                                    + " no fue almacenado correctamente."
                    );
                }

                if (intento < 5) {

                    if (!EstadoUsuario.ACTIVO
                            .name()
                            .equals(estado.estado)) {

                        throw new AssertionError(
                                "La cuenta debería permanecer ACTIVA "
                                        + "antes del quinto intento."
                        );
                    }

                    if (estado.bloqueadoHasta != null) {
                        throw new AssertionError(
                                "No debería existir una fecha de bloqueo "
                                        + "antes del quinto intento."
                        );
                    }

                    System.out.println(
                            "Intento incorrecto "
                                    + intento
                                    + ": cuenta todavía ACTIVA."
                    );

                } else {

                    if (!EstadoUsuario.BLOQUEADO
                            .name()
                            .equals(estado.estado)) {

                        throw new AssertionError(
                                "La cuenta debería quedar BLOQUEADA "
                                        + "en el quinto intento."
                        );
                    }

                    if (estado.bloqueadoHasta == null) {
                        throw new AssertionError(
                                "La cuenta bloqueada debería tener "
                                        + "una fecha de desbloqueo."
                        );
                    }

                    System.out.println(
                            "Intento incorrecto 5: cuenta BLOQUEADA."
                    );
                }
            }
        }

        CredencialesAutenticacionDTO credencialesCorrectas =
                new CredencialesAutenticacionDTO(
                        NOMBRE_USUARIO,
                        CONTRASENA_CORRECTA
                );

        try {
            servicio.autenticar(
                    credencialesCorrectas
            );

            throw new AssertionError(
                    "Una cuenta bloqueada no debería "
                            + "permitir el acceso."
            );

        } catch (SecurityException esperado) {

            EstadoCuenta estado =
                    obtenerEstadoCuenta();

            if (!EstadoUsuario.BLOQUEADO
                    .name()
                    .equals(estado.estado)) {

                throw new AssertionError(
                        "La cuenta debería continuar bloqueada."
                );
            }

            if (estado.intentosFallidos != 5) {
                throw new AssertionError(
                        "El acceso durante el bloqueo no debería "
                                + "modificar los intentos fallidos."
                );
            }

            System.out.println(
                    "Acceso durante el bloqueo: RECHAZADO."
            );
        }
    }

    private static void probarDesbloqueoAutomatico(
            long idUsuario
    ) throws SQLException {

        expirarBloqueo(
                idUsuario
        );

        AutenticacionService servicio =
                new AutenticacionService();

        CredencialesAutenticacionDTO credenciales =
                new CredencialesAutenticacionDTO(
                        NOMBRE_USUARIO,
                        CONTRASENA_CORRECTA
                );

        UsuarioAutenticadoDTO usuario =
                servicio.autenticar(
                        credenciales
                );

        validarUsuarioAutenticado(
                usuario
        );

        EstadoCuenta estado =
                obtenerEstadoCuenta();

        if (!EstadoUsuario.ACTIVO
                .name()
                .equals(estado.estado)) {

            throw new AssertionError(
                    "La cuenta debería volver a estar ACTIVA."
            );
        }

        if (estado.intentosFallidos != 0) {
            throw new AssertionError(
                    "Los intentos deberían reiniciarse "
                            + "después del desbloqueo."
            );
        }

        if (estado.bloqueadoHasta != null) {
            throw new AssertionError(
                    "La fecha de bloqueo debería eliminarse."
            );
        }

        if (estado.ultimoAcceso == null) {
            throw new AssertionError(
                    "El acceso posterior al desbloqueo "
                            + "debería registrar el último acceso."
            );
        }

        System.out.println(
                "Desbloqueo automático y nuevo acceso: CORRECTO."
        );
    }

    private static void probarUsuarioInexistente()
            throws SQLException {

        AutenticacionService servicio =
                new AutenticacionService();

        CredencialesAutenticacionDTO credenciales =
                new CredencialesAutenticacionDTO(
                        "usuario.que.no.existe",
                        CONTRASENA_INCORRECTA
                );

        try {
            servicio.autenticar(
                    credenciales
            );

            throw new AssertionError(
                    "Un usuario inexistente no debería autenticarse."
            );

        } catch (SecurityException esperado) {
            System.out.println(
                    "Usuario inexistente: RECHAZADO."
            );
        }
    }

    private static void validarUsuarioAutenticado(
            UsuarioAutenticadoDTO usuario
    ) {
        if (usuario == null) {
            throw new AssertionError(
                    "El servicio no devolvió un usuario autenticado."
            );
        }

        if (usuario.getIdUsuario() <= 0) {
            throw new AssertionError(
                    "El ID del usuario autenticado no es válido."
            );
        }

        if (usuario.getIdPersona() <= 0) {
            throw new AssertionError(
                    "El ID de la persona autenticada no es válido."
            );
        }

        if (!NOMBRE_USUARIO.equals(
                usuario.getNombreUsuario()
        )) {
            throw new AssertionError(
                    "El nombre de usuario recuperado no coincide."
            );
        }

        if (!usuario.tieneRol(
                "ADMINISTRADOR"
        )) {
            throw new AssertionError(
                    "El usuario debería tener el rol ADMINISTRADOR."
            );
        }

        if (!usuario.isDebeCambiarContrasena()) {
            throw new AssertionError(
                    "El usuario temporal debería tener pendiente "
                            + "el cambio de contraseña."
            );
        }
    }

    private static EstadoCuenta obtenerEstadoCuenta()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_OBTENER_ESTADO
                        )
        ) {
            sentencia.setString(
                    1,
                    NOMBRE_USUARIO
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se encontró la cuenta temporal "
                                    + NOMBRE_USUARIO
                                    + "."
                    );
                }

                return new EstadoCuenta(
                        resultado.getLong(
                                "id_usuario"
                        ),
                        resultado.getString(
                                "estado"
                        ),
                        resultado.getShort(
                                "intentos_fallidos"
                        ),
                        resultado.getObject(
                                "bloqueado_hasta",
                                OffsetDateTime.class
                        ),
                        resultado.getObject(
                                "ultimo_acceso",
                                OffsetDateTime.class
                        ),
                        resultado.getObject(
                                "actualizado_en",
                                OffsetDateTime.class
                        )
                );
            }
        }
    }

    private static void prepararCuentaActiva(
            long idUsuario
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_PREPARAR_CUENTA_ACTIVA
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo preparar la cuenta "
                                + "para la prueba."
                );
            }
        }
    }

    private static void expirarBloqueo(
            long idUsuario
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXPIRAR_BLOQUEO
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo simular el vencimiento "
                                + "del bloqueo."
                );
            }
        }
    }

    private static void restaurarEstadoCuenta(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_RESTAURAR_ESTADO
                        )
        ) {
            sentencia.setString(
                    1,
                    estadoOriginal.estado
            );

            sentencia.setShort(
                    2,
                    estadoOriginal.intentosFallidos
            );

            establecerFechaOpcional(
                    sentencia,
                    3,
                    estadoOriginal.bloqueadoHasta
            );

            establecerFechaOpcional(
                    sentencia,
                    4,
                    estadoOriginal.ultimoAcceso
            );

            establecerFechaOpcional(
                    sentencia,
                    5,
                    estadoOriginal.actualizadoEn
            );

            sentencia.setLong(
                    6,
                    estadoOriginal.idUsuario
            );

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo restaurar el estado "
                                + "original de la cuenta."
                );
            }
        }
    }

    private static void establecerFechaOpcional(
            PreparedStatement sentencia,
            int posicion,
            OffsetDateTime fecha
    ) throws SQLException {

        if (fecha == null) {
            sentencia.setNull(
                    posicion,
                    Types.TIMESTAMP_WITH_TIMEZONE
            );

        } else {
            sentencia.setObject(
                    posicion,
                    fecha
            );
        }
    }

    private static class EstadoCuenta {

        private final long idUsuario;
        private final String estado;
        private final short intentosFallidos;
        private final OffsetDateTime bloqueadoHasta;
        private final OffsetDateTime ultimoAcceso;
        private final OffsetDateTime actualizadoEn;

        private EstadoCuenta(
                long idUsuario,
                String estado,
                short intentosFallidos,
                OffsetDateTime bloqueadoHasta,
                OffsetDateTime ultimoAcceso,
                OffsetDateTime actualizadoEn
        ) {
            this.idUsuario = idUsuario;
            this.estado = estado;
            this.intentosFallidos = intentosFallidos;
            this.bloqueadoHasta = bloqueadoHasta;
            this.ultimoAcceso = ultimoAcceso;
            this.actualizadoEn = actualizadoEn;
        }
    }
}